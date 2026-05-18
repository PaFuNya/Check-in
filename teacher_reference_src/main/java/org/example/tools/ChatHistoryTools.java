package org.example.tools;

import dev.langchain4j.agent.tool.P;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.example.bean.ChatHistory;
import org.example.config.SqlLogConfig;
import org.example.enitity.ChatHistoryEntity;
import org.example.repository.ChatHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ChatHistoryTools {

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;
    
    @Autowired
    private SqlLogConfig sqlLogConfig;

    @Tool("通过sessionId返回用户的对话历史")
    public List<ChatHistory> getChatHistory(@P("用户的sessionId")String sessionId){
        log.info("🔧 [TOOL] getChatHistory被调用");
        log.info("🔧 [TOOL-PARAM] sessionId参数: '{}' (类型: {}, 长度: {})",
            sessionId,
            sessionId != null ? sessionId.getClass().getSimpleName() : "null",
            sessionId != null ? sessionId.length() : 0);

        // 打印sessionId的字节信息，检查是否有隐藏字符
        if (sessionId != null) {
            byte[] bytes = sessionId.getBytes();
            StringBuilder byteStr = new StringBuilder();
            for (byte b : bytes) {
                byteStr.append(String.format("0x%02X ", b));
            }
            log.info("🔧 [TOOL-DEBUG] sessionId字节码: {}", byteStr.toString());
        }

        // 执行数据库查询前的日志
        log.info("🔍 [TOOL] 准备执行数据库查询: findTop20BySessionIdOrderByIdDesc");
        sqlLogConfig.logSqlContext("findTop20BySessionIdOrderByIdDesc", sessionId);


        List<ChatHistory> result = new ArrayList<>();
        List<ChatHistoryEntity> entityList = chatHistoryRepository.findTop20BySessionIdOrderByIdDesc(sessionId);

        log.info("🔍 [TOOL] 数据库查询完成，返回 {} 条记录", entityList.size());

        if (!entityList.isEmpty()) {
            log.info("🔍 [TOOL] 第一条记录详情: id={}, role={}, content='{}', sessionId='{}'",
                entityList.get(0).getId(),
                entityList.get(0).getRole(),
                entityList.get(0).getContent(),
                entityList.get(0).getSessionId());
        }

        for(ChatHistoryEntity entity : entityList){
            ChatHistory chatHistory = new ChatHistory();
            chatHistory.setRole(entity.getRole());
            chatHistory.setContent(entity.getContent());
            result.add(chatHistory);
            log.debug("🔧 [TOOL] 转换记录: role={}, content='{}'", entity.getRole(), entity.getContent());
        }

        log.info("🔧 [TOOL] getChatHistory返回结果数量: {}", result.size());
        log.debug("🔧 [TOOL] getChatHistory完整返回结果: {}", result);
        return result;
//        ChatHistory chatHistory = new ChatHistory();
//        chatHistory.setRole("1");
//        chatHistory.setContent("我叫张三");
//        List<ChatHistory>list =new ArrayList<>();
//        list.add(chatHistory);
//        return list;
    }
    
    /**
     * 测试方法：插入一些测试数据
     */
    public void insertTestData(String sessionId) {
        log.info("🔧 [TEST] 为sessionId '{}' 插入测试数据", sessionId);
        
        ChatHistoryEntity entity1 = new ChatHistoryEntity();
        entity1.setSessionId(sessionId);
        entity1.setRole("0"); // 用户消息
        entity1.setContent("我捡到一辆汽车");
        chatHistoryRepository.save(entity1);
        
        ChatHistoryEntity entity2 = new ChatHistoryEntity();
        entity2.setSessionId(sessionId);
        entity2.setRole("1"); // AI消息
        entity2.setContent("感谢您联系我们来登记失物信息");
        chatHistoryRepository.save(entity2);
        
        log.info("🔧 [TEST] 测试数据插入完成");
    }
}
