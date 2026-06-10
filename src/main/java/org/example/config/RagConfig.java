package org.example.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Slf4j
@Configuration
public class RagConfig {

    private static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private static final String STORE_CACHE_PATH = "data/embedding_store.cache";

    private final ApiKeyConfig apiKeyConfig;

    public RagConfig(ApiKeyConfig apiKeyConfig) {
        this.apiKeyConfig = apiKeyConfig;
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        Path cachePath = Paths.get(STORE_CACHE_PATH);
        if (Files.exists(cachePath)) {
            try {
                log.info("从缓存加载RAG embedding store: {}", cachePath.toAbsolutePath());
                InMemoryEmbeddingStore<TextSegment> store = InMemoryEmbeddingStore.fromFile(STORE_CACHE_PATH);
                log.info("RAG缓存加载成功");
                return store;
            } catch (Exception e) {
                log.warn("缓存加载失败，使用空store: {}", e.getMessage());
            }
        }
        log.info("无RAG缓存，创建空embedding store");
        return new InMemoryEmbeddingStore<>();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(apiKeyConfig.getDashscope().getApiKey())
                .modelName("text-embedding-v3")
                .timeout(Duration.ofMinutes(1))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.5)
                .build();
    }
}
