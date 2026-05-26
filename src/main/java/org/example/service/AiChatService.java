package org.example.service;

import org.example.vo.ChatHistoryVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AiChatService {

    String chatStream(String userId, String studentName, String className, String message);

    Page<ChatHistoryVo> queryChatHistory(String userId, Pageable page);

    void clearChatHistory(String userId);

    String embeddingIndex();

    List<String> embeddingQuery(String message);
}
