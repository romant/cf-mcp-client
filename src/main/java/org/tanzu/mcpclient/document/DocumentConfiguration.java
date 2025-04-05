package org.tanzu.mcpclient.document;

import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentConfiguration {

    private VectorStore vectorStore;

    public DocumentConfiguration(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Bean
    public String getEmbeddingModel() {
        CfEnv cfEnv = new CfEnv();

        return cfEnv.findServicesByLabel("genai").stream()
                .filter(this::isEmbeddingService)
                .findFirst()
                .map(CfService::getName)
                .orElse("");
    }

    private boolean isEmbeddingService(CfService service) {
        return service.getCredentials() != null &&
                service.getCredentials().getString("model_capabilities") != null &&
                service.getCredentials().getString("model_capabilities").contains("embedding");
    }

    @Bean
    public String getVectorDatabase() {
        if (vectorStore == null) {
            return "";
        }
        return vectorStore.getName();
    }
}
