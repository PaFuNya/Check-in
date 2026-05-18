package org.example.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * RAG相关配置类
 * 使用OpenAI兼容模式访问通义千问的向量化服务
 */
@Configuration
public class RagConfig {

    private static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private static final String API_KEY = "sk-55ed1b102e2a465f9e59a954a812dfe0";

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(){
        return  new InMemoryEmbeddingStore<>();
    }

    /**
     * 配置向量化模型
     * 使用OpenAI兼容模式，支持完整的请求和响应日志
     */
    @Bean
    public EmbeddingModel embeddingModel(){
        System.out.println("🔥 创建OpenAI兼容模式 EmbeddingModel Bean");
        return OpenAiEmbeddingModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(API_KEY)
                .modelName("text-embedding-v3")
                .timeout(Duration.ofMinutes(1))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore,EmbeddingModel embeddingModel){
        return EmbeddingStoreContentRetriever.builder().
                embeddingStore(embeddingStore).
                embeddingModel(embeddingModel).
                maxResults(2).minScore(0.5).build();
    }
}
