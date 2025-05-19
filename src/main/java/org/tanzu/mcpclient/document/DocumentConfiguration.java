package org.tanzu.mcpclient.document;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tanzu.mcpclient.util.GenAIService;

@Configuration
public class DocumentConfiguration {

    private final VectorStore vectorStore;
    private final String embeddingModel;

    public DocumentConfiguration(VectorStore vectorStore, GenAIService genAIServiceUtil) {
        this.vectorStore = vectorStore;
        this.embeddingModel = genAIServiceUtil.getEmbeddingModelName();
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    @Bean
    public String getVectorDatabase() {
        return vectorStore.getName();
    }
}