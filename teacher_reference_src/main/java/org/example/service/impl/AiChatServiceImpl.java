package org.example.service.impl;

import cn.hutool.core.io.FileUtil;
import org.example.util.JsonUtil;
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
import org.example.aioutput.IntentionOuput;
import org.example.aioutput.LostPropertyOutput;
import org.example.aioutput.LostRegisterOutput;
import org.example.aiservice.AiAssistant;
import org.example.aiservice.QueryLostPropertyAssistant;
import org.example.aiservice.RegisterFindLostPropertyAssistant;
import org.example.aiservice.RegisterLostAssistant;
import org.example.aop.ChatFlow;
import org.example.enitity.ChatHistoryEntity;
import org.example.enitity.LostPropertyEntity;
import org.example.enitity.LostRegisterEntity;
import org.example.repository.ChatHistoryRepository;
import org.example.repository.LostPropertyRepository;
import org.example.repository.LostRegisterRepository;
import org.example.service.AiChatService;
import org.example.vo.ChatHistoryVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {


    @Autowired
    private AiAssistant assistant;

//    @Autowired
//    private AiIntentionAssistant aiIntentionAssistant;

    @Autowired
    private LostRegisterRepository lostRegisterRepository;

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private RegisterFindLostPropertyAssistant lostPropertyAssistant;

    @Autowired
    private QueryLostPropertyAssistant queryLostPropertyAssistant;

    @Autowired
    private LostPropertyRepository lostPropertyRepository;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore embeddingStore;

    @Autowired
    private RegisterLostAssistant registerLost;

    @Override
    @ChatFlow
    public String chatStream(String userId, String message) {
        //用户意图识别
        log.info("🎯 调用意图识别 - userId: {}, message: {}", userId, message);
        log.info("🔧 使用的assistant实例: {}", assistant.getClass().getName());

        // 强制刷新日志缓冲区
        System.out.println("=== 准备调用大模型进行意图识别 ===");
        System.out.flush();

        long startTime = System.currentTimeMillis();
        IntentionOuput intention = assistant.intention(userId, message);
        long endTime = System.currentTimeMillis();

        System.out.println("=== 大模型调用完成 ===");
        System.out.flush();

        log.info("🎯 意图识别结果: {}", intention);
        log.info("⏱️ 意图识别耗时: {}ms", endTime - startTime);
        String output = intention.getOutput();
        switch (intention.getIntention()) {
            case 1:
                //丢失信息登记
                output = registerLost(userId, message);
                break;
            case 2:
                //找到失物登记
                output = findLostPropertyRegister(userId, message);
                break;
            case 3:
                //失物查询
                output = queryLostProperty(userId, message);
                break;
            default:
                //其他
                return output;
        }
        return output;

    }

    private String queryLostProperty(String userId, String message) {
        StringBuffer sb = new StringBuffer();
        queryLostPropertyAssistant.queryLostProperty(userId, message).doOnNext(sb::append).blockLast();
        log.info("----queryLostProperty----" + sb);
        return sb.toString();
    }

    private String findLostPropertyRegister(String userId, String message) {
        log.info("🔥 开始调用findLostPropertyRegister - userId: {}, message: {}", userId, message);

        LostPropertyOutput out = lostPropertyAssistant.registerLostProperty(userId, message);
        log.info("✅ 流式调用完成，完整响应: {}", out);

        Long outId = out.getId();
        if (out.getCompleted()) {
            if (outId != null && outId > 0) {
                // 修改登记：按id加载并进行“非空覆盖”式更新
                LostPropertyEntity entity = lostPropertyRepository.findById(outId).orElseGet(LostPropertyEntity::new);

                if (out.getUsername() != null && !out.getUsername().isBlank()) {
                    entity.setUsername(out.getUsername());
                }
                if (out.getPhone() != null && !out.getPhone().isBlank()) {
                    entity.setPhone(out.getPhone());
                }
                if (out.getLostName() != null && !out.getLostName().isBlank()) {
                    entity.setLostName(out.getLostName());
                }
                if (out.getLostType() != null && !out.getLostType().isBlank()) {
                    entity.setLostType(out.getLostType());
                }
                // 修改登记：即使未完成也允许保存当前修改
                lostPropertyRepository.save(entity);

            } else  {
                // 新增登记：仅在完成时落库；忽略id避免“id=0”导致merge异常
                LostPropertyEntity entity = new LostPropertyEntity();
                BeanUtils.copyProperties(out, entity, "id");
                lostPropertyRepository.save(entity);
            }
        }


        return out.getOutput();
    }

    @Override
    public Page<ChatHistoryVo> queryChatHistory(String userId, Pageable page) {
        // 准备VO列表
        List<ChatHistoryVo> voList = new ArrayList<>();

        // 遍历实体并转换为VO
        Page<ChatHistoryEntity> entityPage = chatHistoryRepository.findAllBySessionId(userId, page);

        for (ChatHistoryEntity entity : entityPage.getContent()) {
            // 过滤掉非USER角色
            ChatHistoryVo vo = ChatHistoryVo.of(
                    entity.getRole(),
                    entity.getContent(),
                    entity.getCreatedDate()
            );
            voList.add(vo);
        }

        // 重新构建分页对象
        return new PageImpl<>(
                voList,                          // VO列表
                entityPage.getPageable(),        // 保持分页信息
                entityPage.getTotalElements()    // 保持总记录数
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearChatHistory(String userId) {
        chatHistoryRepository.deleteBySessionId(userId);
    }

    @Override
    public String embeddingIndex() {
        //将找到失物登记的数据进行查询并且写入到一个文件中去
        String path = "/Users/Zhuanz/Desktop/data/data.txt";
        FileUtil.writeString("", path, StandardCharsets.UTF_8);
        List<String> collect = StreamSupport.stream(lostPropertyRepository.findAll().spliterator(), false)
                .map(JsonUtil::toJson)
                .toList();
        FileUtil.appendLines(collect, path, StandardCharsets.UTF_8);
        //加载并解析文档的内容
        DocumentParser documentParser = new TextDocumentParser();
        //加载
        Document document = FileSystemDocumentLoader.loadDocument(FileUtil.getAbsolutePath(path), documentParser);
        //定义文档分割器
        DocumentSplitter splitter = new DocumentByLineSplitter(200, 100);
        List<TextSegment> split = splitter.split(document);
        //进行向量化并且存储到向量数据库里面
        List<Embedding> content = embeddingModel.embedAll(split).content();
        embeddingStore.addAll(content, split);
        return "success";
    }

    @Override
    public List<String> embeddingQuery(String message) {
        //构造检索条件
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder().
                queryEmbedding(embeddingModel.embed(message).content())
                .maxResults(2).minScore(0.8).build();
        //进行向量检索
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        //返回向量检索结果
        return result.matches().stream().map(x -> x.embedded().text()).collect(Collectors.toList());
    }


    private String registerLost(String userId, String message) {
        log.info("📝 调用丢失物品登记 - userId: {}, message: {}", userId, message);
        LostRegisterOutput lostRegisterOutput = registerLost.registerLost(userId, message);
        log.info("📝 丢失物品登记结果: {}", lostRegisterOutput);
        if (lostRegisterOutput.getCompleted()) {
            //持久化数据库
            LostRegisterEntity entity = new LostRegisterEntity();
            BeanUtils.copyProperties(lostRegisterOutput, entity);
            lostRegisterRepository.save(entity);
        }
        return lostRegisterOutput.getOutput();
    }
}
