package org.example.config;

import lombok.extern.slf4j.Slf4j;
import org.example.service.AiChatService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RagStartupLoader implements ApplicationRunner {

    private final AiChatService aiChatService;

    public RagStartupLoader(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("正在加载学生手册知识库...");
            String result = aiChatService.embeddingIndex();
            log.info("学生手册知识库加载完成: {}", result);
        } catch (Exception e) {
            log.error("学生手册知识库加载失败", e);
        }
    }
}
