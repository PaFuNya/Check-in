package org.example.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.example.bean.ChatHistory;
import org.example.entity.ChatHistoryEntity;
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

    @Tool("通过sessionId返回用户的对话历史")
    public List<ChatHistory> getChatHistory(@P("用户的sessionId") String sessionId) {
        log.info("🔧 [TOOL] getChatHistory被调用, sessionId: {}", sessionId);

        List<ChatHistory> result = new ArrayList<>();
        List<ChatHistoryEntity> entityList = chatHistoryRepository.findTop20BySessionIdOrderByIdDesc(sessionId);

        log.info("🔍 [TOOL] 数据库查询完成，返回 {} 条记录", entityList.size());

        for (ChatHistoryEntity entity : entityList) {
            ChatHistory chatHistory = new ChatHistory();
            chatHistory.setRole(entity.getRole());
            chatHistory.setContent(entity.getContent());
            result.add(chatHistory);
        }

        return result;
    }
}
