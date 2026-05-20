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
import org.example.aiservice.AiAssistant;
import org.example.aiservice.CheckInAssistant;
import org.example.aiservice.LeaveRequestAssistant;
import org.example.aiservice.QueryCheckInAssistant;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    @Autowired
    private AiAssistant assistant;

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

    @Override
    @ChatFlow
    public String chatStream(String userId, String studentName, String className, String message) {
        log.info("intention - userId: {}, studentName: {}, message: {}", userId, studentName, message);

        IntentionOutput intention = assistant.intention(userId, message, userId,
                studentName != null ? studentName : "",
                className != null ? className : "");
        log.info("intention result: {}", intention);

        String output = intention.getOutput();
        switch (intention.getIntention()) {
            case 1:
                output = doLeaveRequest(userId, message);
                break;
            case 2:
                output = doQueryCheckIn(userId, message);
                break;
            case 3:
                output = doRagQuery(userId, message);
                break;
            case 4:
            case 5:
            default:
                return output;
        }
        return output;
    }

    private String doCheckIn(String userId, String message) {
        log.info("📝 调用签到登记 - userId: {}, message: {}", userId, message);
        CheckInOutput out = checkInAssistant.checkIn(userId, message);
        log.info("📝 签到登记结果: {}", out);

        if (out.getCompleted() != null && out.getCompleted()) {
            CheckInRecordEntity entity = new CheckInRecordEntity();
            entity.setStudentId(out.getStudentId());
            entity.setStudentName(out.getStudentName());
            entity.setRoomNumber(out.getRoomNumber());
            entity.setDormBuilding(out.getDormBuilding());
            entity.setLocationInfo(out.getLocationInfo());
            entity.setStatus("已签到");
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
            entity.setAuditStatus("待审核");
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
        List<String> results = embeddingQuery(message);
        if (results.isEmpty()) {
            return "抱歉，未找到相关的寝室管理规定。建议你咨询辅导员获取更多信息。";
        }
        StringBuilder sb = new StringBuilder("根据寝室管理规定：\n");
        for (int i = 0; i < results.size(); i++) {
            sb.append((i + 1)).append(". ").append(results.get(i)).append("\n");
        }
        return sb.toString();
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
        DocumentSplitter splitter = new DocumentByLineSplitter(200, 100);
        List<TextSegment> split = splitter.split(document);
        List<Embedding> content = embeddingModel.embedAll(split).content();
        embeddingStore.addAll(content, split);
        return "success";
    }

    @Override
    public List<String> embeddingQuery(String message) {
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed(message).content())
                .maxResults(2)
                .minScore(0.8)
                .build();
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        return result.matches().stream()
                .map(x -> x.embedded().text())
                .collect(Collectors.toList());
    }
}
