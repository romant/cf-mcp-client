package org.tanzu.mcpclient.document;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class DocumentConfiguration {

    private static final String MODEL_CAPABILITIES = "model_capabilities";
    private static final String MODEL_NAME = "model_name";
    private static final String EMBEDDING_CAPABILITY = "embedding";
    private static final String GENAI_LABEL = "genai";

    private final VectorStore vectorStore;
    private final String embeddingModel;

    public DocumentConfiguration(VectorStore vectorStore) {
        this.vectorStore = vectorStore;

        CfEnv cfEnv = new CfEnv();
        this.embeddingModel = cfEnv.findServicesByLabel(GENAI_LABEL).stream()
                .filter(this::isEmbeddingService)
                .findFirst()
                .map(cfService -> {
                    CfCredentials credentials = cfService.getCredentials();
                    String modelName = credentials != null ? credentials.getString(MODEL_NAME) : null;
                    return modelName != null ? modelName : cfService.getName();
                })
                .orElse("");
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    private boolean isEmbeddingService(CfService service) {
        return Optional.ofNullable(service.getCredentials())
                .map(creds -> creds.getString(MODEL_CAPABILITIES))
                .filter(capabilities -> capabilities.contains(EMBEDDING_CAPABILITY))
                .isPresent();
    }

    @Bean
    public String getVectorDatabase() {
        if (vectorStore == null) {
            return "";
        }
        return vectorStore.getName();
    }
}
