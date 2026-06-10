package org.example.service.impl;

import cn.hutool.core.io.FileUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.example.aioutput.CheckInOutput;
import org.example.aioutput.IntentionOutput;
import org.example.aioutput.LeaveRequestOutput;
import org.example.enums.AuditStatus;
import org.example.enums.CheckInStatus;
import org.example.aiservice.AiAssistant;
import org.example.aiservice.CheckInAssistant;
import org.example.aiservice.LeaveRequestAssistant;
import org.example.aiservice.QueryCheckInAssistant;
import org.example.aiservice.SimpleChatAssistant;
import org.example.aop.ChatFlow;
import org.example.entity.ChatHistoryEntity;
import org.example.entity.CheckInRecordEntity;
import org.example.entity.LeaveRequestEntity;
import org.example.repository.ChatHistoryRepository;
import org.example.repository.CheckInRecordRepository;
import org.example.repository.LeaveRequestRepository;
import org.example.service.AiChatService;
import org.example.util.JsonUtil;
import org.example.vo.ChatHistoryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    @Autowired
    private AiAssistant aiAssistant;

    @Autowired
    private SimpleChatAssistant simpleChatAssistant;

    @Autowired
    private CheckInAssistant checkInAssistant;

    @Autowired
    private LeaveRequestAssistant leaveRequestAssistant;

    @Autowired
    private QueryCheckInAssistant queryCheckInAssistant;

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private CheckInRecordRepository checkInRecordRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    // PDF 文本缓存（带页码标记）
    private static final String PDF_TEXT_CACHE = "data/handbook_tagged.txt";
    private List<String> handbookLines = null;
    private List<String> handbookParagraphs = null;

    @Override
    @ChatFlow
    public String chatStream(String userId, String studentName, String className, String message) {
        log.info("📝 用户输入 - userId: {}, message: {}", userId, message);

        IntentionOutput intentionOutput = aiAssistant.intention(userId, message, userId, studentName, className);
        log.info("intention result: {}", intentionOutput);

        int intention = intentionOutput.getIntention();

        switch (intention) {
            case 1: // 请假报备
                return doLeaveRequest(userId, message);
            case 2: // 状态查询（签到记录、请假状态）
                return doQueryCheckIn(userId, message);
            case 3: // 规则问答 → RAG 知识库
                return doRagQuery(userId, message);
            case 4: // 闲聊/无法处理
                return intentionOutput.getOutput();
            case 5: // 个人信息
                return intentionOutput.getOutput();
            default:
                return intentionOutput.getOutput();
        }
    }

    private String doCheckIn(String userId, String studentName, String className, String message) {
        log.info("📝 调用签到 - userId: {}, message: {}", userId, message);
        CheckInOutput out = checkInAssistant.checkIn(userId, message);
        log.info("📝 签到结果: {}", out);

        if (out.getCompleted() != null && out.getCompleted()) {
            CheckInRecordEntity entity = new CheckInRecordEntity();
            entity.setStudentId(out.getStudentId());
            entity.setStudentName(out.getStudentName());
            entity.setRoomNumber(out.getRoomNumber());
            entity.setDormBuilding(out.getDormBuilding());
            entity.setLocationInfo(out.getLocationInfo());
            entity.setStatus(CheckInStatus.CHECKED_IN.getCode());
            entity.setCheckTime(new Date());
            checkInRecordRepository.save(entity);
        }
        return out.getOutput();
    }

    private String doLeaveRequest(String userId, String message) {
        log.info("📝 调用请假报备 - userId: {}, message: {}", userId, message);
        LeaveRequestOutput out = leaveRequestAssistant.leaveRequest(userId, message);
        log.info("📝 请假报备结果: {}", out);

        if (out.getCompleted() != null && out.getCompleted()) {
            LeaveRequestEntity entity = new LeaveRequestEntity();
            entity.setStudentId(out.getStudentId());
            entity.setStudentName(out.getStudentName());
            entity.setLeaveType(out.getLeaveType());
            entity.setReason(out.getReason());
            entity.setAuditStatus(AuditStatus.PENDING.getCode());
            entity.setAuditorComment("");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            try {
                if (out.getStartTime() != null && !out.getStartTime().isBlank()) {
                    LocalDateTime startLdt = LocalDateTime.parse(out.getStartTime(), formatter);
                    entity.setStartTime(Date.from(startLdt.atZone(ZoneId.systemDefault()).toInstant()));
                }
                if (out.getEndTime() != null && !out.getEndTime().isBlank()) {
                    LocalDateTime endLdt = LocalDateTime.parse(out.getEndTime(), formatter);
                    entity.setEndTime(Date.from(endLdt.atZone(ZoneId.systemDefault()).toInstant()));
                }
            } catch (Exception e) {
                log.error("❌ 解析请假时间失败: {}", e.getMessage());
            }

            leaveRequestRepository.save(entity);
        }
        return out.getOutput();
    }

    private String doQueryCheckIn(String userId, String message) {
        StringBuffer sb = new StringBuffer();
        queryCheckInAssistant.queryCheckIn(userId, message).doOnNext(sb::append).blockLast();
        log.info("----queryCheckIn----" + sb);
        return sb.toString();
    }

    private String doRagQuery(String userId, String message) {
        // 使用带页码的关键词搜索
        List<String> results = searchHandbook(message);

        if (results.isEmpty()) {
            // 没找到相关内容，用 AI 生成通用回复
            try {
                String prompt = "你是浙江国际海运职业技术学院的寝室签到助手。学生问了一个关于寝室管理规定的问题，但没有找到具体的相关条文。\n" +
                        "请基于你对高校寝室管理的一般了解，给出一个有帮助的回复，并建议学生查阅学生手册或咨询辅导员。\n\n" +
                        "学生问题：" + message;
                return simpleChatAssistant.chat(prompt);
            } catch (Exception e) {
                return "抱歉，未找到相关的寝室管理规定。建议你查阅学生手册或咨询辅导员获取更多信息。";
            }
        }

        // 找到相关内容，用 AI 组织正式回答
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            context.append(results.get(i)).append("\n\n");
        }

        String prompt = "你是浙江国际海运职业技术学院的寝室签到助手。请根据以下学生手册中的相关规定，回答学生的问题。\n\n" +
                "要求：\n" +
                "1. 用正式、友好的语气回答\n" +
                "2. 引用具体的条文内容，说明出处（如「根据《浙江国际海运职业技术学院学生寝室管理办法》第XXX页」）\n" +
                "3. 如果找到多个相关条文，都要提及\n" +
                "4. 结尾可以补充一句温馨提示\n\n" +
                "--- 学生手册相关条文 ---\n" + context +
                "--- 问题 ---\n" + message;

        log.info("RAG 上下文: {}", context);
        try {
            return simpleChatAssistant.chat(prompt);
        } catch (Exception e) {
            log.error("AI 生成回复失败，返回原始文本", e);
            // AI 调用失败，回退到原始文本
            StringBuilder sb = new StringBuilder("根据学生手册相关规定：\n");
            for (int i = 0; i < results.size(); i++) {
                sb.append((i + 1)).append(". ").append(results.get(i)).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * 关键词搜索学生手册文本（带页码）
     * 返回格式: "第XXX页：匹配文本"
     */
    private List<String> searchHandbook(String query) {
        // 懒加载带页码的文本
        if (handbookLines == null) {
            loadHandbookText();
        }
        if (handbookLines == null || handbookLines.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取有意义的关键词（按常见虚词分割）
        String[] stopWords = {"的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这", "那", "吗", "呢", "吧", "啊", "可以", "能", "什么", "怎么", "如何", "哪", "为什么", "多少", "几"};
        
        // 先清理查询
        String cleanQuery = query.replaceAll("[\\p{Punct}\\s]", "");
        
        // 按虚词分割，提取核心关键词
        List<String> keywords = new ArrayList<>();
        String remaining = cleanQuery;
        for (String sw : stopWords) {
            remaining = remaining.replace(sw, "|");
        }
        for (String part : remaining.split("\\|")) {
            if (part.length() >= 2) {
                keywords.add(part);
            }
        }
        
        // 如果没有提取到关键词，用原始查询的 2-gram
        if (keywords.isEmpty()) {
            for (int i = 0; i <= cleanQuery.length() - 2; i++) {
                keywords.add(cleanQuery.substring(i, i + 2));
            }
        }

        if (keywords.isEmpty()) return Collections.emptyList();

        log.info("搜索关键词: {}", keywords);

        // 按关键词匹配度排序（支持组合加分）
        List<Map.Entry<String, Integer>> scored = new ArrayList<>();
        for (String line : handbookLines) {
            // 去掉页码标记后计算匹配分数
            String text = line.replaceAll("^\\[p\\d+\\] ", "");
            int score = 0;
            int matchedKeywords = 0; // 匹配到的关键词数量
            for (String kw : keywords) {
                if (kw.isEmpty()) continue;
                int idx = 0, count = 0;
                while ((idx = text.indexOf(kw, idx)) != -1) {
                    count++;
                    idx += kw.length();
                }
                if (count > 0) {
                    matchedKeywords++;
                    // 关键词越长，权重越高
                    score += count * kw.length() * kw.length();
                }
            }
            // 组合加分：匹配到多个关键词的行获得指数级加分
            // 这样"同时包含寝室+宠物"的行会远高于"只包含寝室"的行
            if (matchedKeywords > 1) {
                score *= (matchedKeywords * matchedKeywords);
            }
            if (score > 0) {
                scored.add(Map.entry(line, score));
            }
        }

        // 按分数降序排序
        scored.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        // 取前5条，合并同页内容
        Map<String, String> pageResults = new LinkedHashMap<>();
        for (var entry : scored) {
            String line = entry.getKey();
            // 提取页码
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^\\[p(\\d+)\\] ").matcher(line);
            String page = "?";
            String text = line;
            if (m.find()) {
                page = m.group(1);
                text = line.substring(m.end());
            }
            // 合并同一页的内容
            pageResults.merge(page, text, (a, b) -> a + "；" + b);
            if (pageResults.size() >= 5) break;
        }

        // 格式化结果
        List<String> results = new ArrayList<>();
        for (var entry : pageResults.entrySet()) {
            results.add("第" + entry.getKey() + "页：" + entry.getValue());
        }
        return results;
    }

    /**
     * 加载带页码的文本
     */
    private void loadHandbookText() {
        Path textPath = Paths.get(PDF_TEXT_CACHE);
        if (Files.exists(textPath)) {
            try {
                List<String> lines = Files.readAllLines(textPath, StandardCharsets.UTF_8);
                handbookLines = lines.stream()
                        .filter(l -> l.length() > 10)
                        .collect(Collectors.toList());
                log.info("加载学生手册带页码文本: {} 行", handbookLines.size());
                return;
            } catch (Exception e) {
                log.warn("加载文本缓存失败: {}", e.getMessage());
            }
        }
        handbookLines = Collections.emptyList();
    }

    @Override
    public Page<ChatHistoryVo> queryChatHistory(String userId, Pageable page) {
        List<ChatHistoryVo> voList = new ArrayList<>();
        Page<ChatHistoryEntity> entityPage = chatHistoryRepository.findAllBySessionId(userId, page);

        for (ChatHistoryEntity entity : entityPage.getContent()) {
            ChatHistoryVo vo = ChatHistoryVo.of(
                    entity.getRole(),
                    entity.getContent(),
                    entity.getCreatedDate()
            );
            voList.add(vo);
        }

        return new PageImpl<>(voList, entityPage.getPageable(), entityPage.getTotalElements());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearChatHistory(String userId) {
        chatHistoryRepository.deleteBySessionId(userId);
    }

    @Override
    public String embeddingIndex() {
        // 先提取 PDF 文本（供关键词搜索用）
        loadHandbookText();

        // embedding 索引（可选，如果 DashScope 可用）
        String pdfPath = System.getProperty("user.dir") + "/data/student_handbook.pdf";
        File pdfFile = new File(pdfPath);

        if (!pdfFile.exists()) {
            log.info("未找到 PDF，使用硬编码规则");
            String path = System.getProperty("user.dir") + "/data/rules_data.txt";
            FileUtil.writeString("", path, StandardCharsets.UTF_8);
            List<String> rules = List.of(
                    "寝室关门时间：每晚23:00，晚归记为违纪",
                    "晚归三次以上给予警告处分",
                    "夜不归宿一次给予严重警告处分",
                    "寝室内禁止使用大功率电器（超过800W）",
                    "请假需提前一天向辅导员提交申请",
                    "病假需提供医院证明",
                    "未经批准擅自离校按旷课处理",
                    "每周日晚进行寝室卫生检查"
            );
            FileUtil.appendLines(rules, path, StandardCharsets.UTF_8);
            DocumentParser documentParser = new TextDocumentParser();
            Document document = FileSystemDocumentLoader.loadDocument(FileUtil.getAbsolutePath(path), documentParser);
            DocumentSplitter splitter = new DocumentByLineSplitter(500, 100);
            List<TextSegment> split = splitter.split(document);
            List<Embedding> content = embeddingModel.embedAll(split).content();
            embeddingStore.addAll(content, split);
            return "success（硬编码规则）";
        }

        try {
            log.info("正在构建 embedding 索引...");
            DocumentParser parser = new dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser();
            Document document = FileSystemDocumentLoader.loadDocument(pdfFile.toPath(), parser);
            DocumentSplitter splitter = new DocumentByLineSplitter(500, 100);
            List<TextSegment> split = splitter.split(document);
            log.info("文档分为 {} 个片段，分批嵌入...", split.size());

            int batchSize = 10; // DashScope 限制每批最多10个
            for (int i = 0; i < split.size(); i += batchSize) {
                int end = Math.min(i + batchSize, split.size());
                List<TextSegment> batch = split.subList(i, end);
                List<Embedding> embeddings = embeddingModel.embedAll(batch).content();
                embeddingStore.addAll(embeddings, batch);
                if ((i / batchSize) % 10 == 0) {
                    log.info("已嵌入 {}/{} 个片段", end, split.size());
                }
            }
            return "success，共索引 " + split.size() + " 个片段";
        } catch (Exception e) {
            log.warn("Embedding 索引失败（关键词搜索仍可用）: {}", e.getMessage());
            return "embedding 索引失败，但关键词搜索已可用";
        }
    }

    @Override
    public List<String> embeddingQuery(String message) {
        try {
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(embeddingModel.embed(message).content())
                    .maxResults(3)
                    .minScore(0.7)
                    .build();
            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
            return result.matches().stream()
                    .map(x -> x.embedded().text())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.debug("Embedding 查询失败，回退到关键词搜索: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
