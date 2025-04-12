package org.tanzu.mcpclient.document;

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
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
public class VectorStoreConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(VectorStoreConfiguration.class);

    @Bean
    @Conditional(VectorStoreCondition.class)
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        int dimensions = embeddingModel.dimensions();
        logger.info("Embedding dimensions: {}", dimensions);
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                    .dimensions(dimensions)                    // Optional: defaults to model dimensions or 1536
                    .distanceType(COSINE_DISTANCE)       // Optional: defaults to COSINE_DISTANCE
                    .indexType(HNSW)                     // Optional: defaults to HNSW
                    .initializeSchema(true)              // Optional: defaults to false
                    .schemaName("public")                // Optional: defaults to "public"
                    .vectorTableName("vector_store")     // Optional: defaults to "vector_store"
                    .maxDocumentBatchSize(10000)         // Optional: defaults to 10000
                    .removeExistingVectorStoreTable(true)
                    .initializeSchema(true)
                    .build();
    }

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore fallbackVectorStore() {
        logger.info("Creating fallback vectorStore bean");
        return new EmptyVectorStore();
    }

    public static class VectorStoreCondition implements Condition {
        private static final Logger logger = LoggerFactory.getLogger(VectorStoreCondition.class);

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            try {
                // Get environment from the condition context
                Environment env = context.getEnvironment();

                // Test direct JDBC connection
                return testDirectJdbcConnection(env);
            } catch (Exception e) {
                logger.error("Failed to establish JDBC connection for vectorStore, skipping bean creation", e);
                return false;
            }
        }

        private boolean testDirectJdbcConnection(Environment env) {
            // Get JDBC properties from environment
            String url = env.getProperty("spring.datasource.url");
            String username = env.getProperty("spring.datasource.username");
            String password = env.getProperty("spring.datasource.password");
            String driverClassName = env.getProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");

            if (url == null) {
                logger.warn("No datasource URL found in environment properties");
                return false;
            }

            Connection connection = null;
            try {
                // Load the driver
                Class.forName(driverClassName);

                // Create direct connection
                connection = DriverManager.getConnection(url, username, password);
                boolean valid = connection.isValid(5); // 5 second timeout

                if (valid) {
                    try (Statement stmt = connection.createStatement()) {
                        // Execute a simple query to verify DB is operational
                        stmt.execute("SELECT 1");
                    }
                }

                logger.info("JDBC connection test: {}", valid ? "PASSED" : "FAILED");
                return valid;
            } catch (ClassNotFoundException e) {
                logger.error("JDBC driver not found: {} : {}", driverClassName, e.getMessage());
                return false;
            } catch (SQLException e) {
                logger.error("Direct JDBC connection failed: {}", e.getMessage());
                return false;
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        logger.warn("Error closing JDBC connection: {}", e.getMessage());
                    }
                }
            }
        }
    }

    public static class EmptyVectorStore implements VectorStore {
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
