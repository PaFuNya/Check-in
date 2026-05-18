package org.example.service;

import org.example.vo.ChatHistoryVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AiChatService {
    /**
     * 主要针对用户意图进行流程编排
     * @param userId
     * @param message
     */
   String chatStream(String userId, String message);

    /**
     * 查询聊天历史记录
     * @param userId
     * @param page
     * @return
     */
    Page<ChatHistoryVo> queryChatHistory(String userId, Pageable page);

    /**
     * 删除聊天历史记录
     * @param userId
     */
    void clearChatHistory(String userId);

    public String embeddingIndex();

    List<String> embeddingQuery(String message);
}
