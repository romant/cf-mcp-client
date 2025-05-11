package org.tanzu.mcpclient.memory;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.tanzu.mcpclient.document.VectorStoreConfiguration;

@Configuration
public class MemoryConfiguration {
    private final JdbcTemplate jdbcTemplate;

    public MemoryConfiguration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Bean
    @Conditional(VectorStoreConfiguration.VectorStoreCondition.class)
    public ChatMemoryRepository chatMemoryRepository() {
        return JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public ChatMemoryRepository transientMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }


}
