//package org.example.config;
//
//import dev.langchain4j.model.chat.ChatLanguageModel;
//import dev.langchain4j.model.chat.StreamingChatLanguageModel;
//import dev.langchain4j.model.openai.OpenAiChatModel;
//import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.time.Duration;
//
///**
// * 聊天模型配置类
// * 使用OpenAI兼容模式访问通义千问，支持完整的日志功能
// */
//@Configuration
//public class ChatConfig {
//
//    public static final String QAQ_CHAT_BEAN = "qwenStreamingChatModel";
//    private static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
//    private static final String API_KEY = "sk-55ed1b102e2a465f9e59a954a812dfe0";
//
//    /**
//     * 配置普通聊天模型（非流式）
//     * 使用OpenAI兼容模式，支持完整的请求和响应日志
//     */
//    @Bean("qwenChatModel")
//    public ChatLanguageModel qwenChatModel() {
//        System.out.println("🔥 创建OpenAI兼容模式 qwenChatModel Bean");
//        return OpenAiChatModel.builder()
//                .baseUrl(BASE_URL)
//                .apiKey(API_KEY)
//                .modelName("qwen-plus")
//                .timeout(Duration.ofMinutes(2))
//                .logRequests(true)
//                .logResponses(true)
//                .build();
//    }
//
//    /**
//     * 配置流式聊天模型
//     * 使用OpenAI兼容模式，支持完整的请求和响应日志
//     */
//    @Bean(QAQ_CHAT_BEAN)
//    public StreamingChatLanguageModel qwenStreamingChatModel() {
//        System.out.println("🔥 创建OpenAI兼容模式 qwenStreamingChatModel Bean");
//        return OpenAiStreamingChatModel.builder()
//                .baseUrl(BASE_URL)
//                .apiKey(API_KEY)
//                .modelName("qwen-plus")
//                .timeout(Duration.ofMinutes(2))
//                .logRequests(true)
//                .logResponses(true)
//                .build();
//    }
//
//}