package org.tanzu.mcpclient.vectorstore;

import io.pivotal.cfenv.core.CfEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
public class VectorStoreConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(VectorStoreConfiguration.class);

    @Bean
    @Conditional(DatabaseAvailableCondition.class) // Use the simplified condition
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {

        int dimensions = PgVectorStore.OPENAI_EMBEDDING_DIMENSION_SIZE;
        if ( isEmbeddingModelAvailable() ) {
            dimensions = embeddingModel.dimensions();
        }
        logger.info("Embedding dimensions: {}", dimensions);

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(dimensions)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("vector_store")
                .maxDocumentBatchSize(10000)
                .initializeSchema(true)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore fallbackVectorStore() {
        logger.info("Creating fallback vectorStore bean");
        return new EmptyVectorStore();
    }

    private boolean isEmbeddingModelAvailable() {
        try {
            CfEnv cfEnv = new CfEnv();
            return cfEnv.findServicesByLabel("genai").stream()
                    .anyMatch(service -> {
                        if (service.getCredentials() == null) return false;
                        String capabilities = service.getCredentials().getString("model_capabilities");
                        return capabilities != null && capabilities.contains("embedding");
                    });
        } catch (Exception e) {
            logger.warn("Error checking for embedding model availability: {}", e.getMessage());
            return false;
        }
    }

    // Keep the EmptyVectorStore inner class
    public static class EmptyVectorStore implements VectorStore {
        // Implementation remains the same
        @Override
        public void add(List<Document> documents) {
        }

        @Override
        public void delete(List<String> idList) {
        }

        @Override
        public void delete(Filter.Expression filterExpression) {
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            return List.of();
        }

        @Override
        public String getName() {
            return "";
        }
    }
}