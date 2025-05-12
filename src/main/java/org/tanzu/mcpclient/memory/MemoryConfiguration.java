package org.tanzu.mcpclient.memory;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.tanzu.mcpclient.document.VectorStoreConfiguration;

@Configuration
public class MemoryConfiguration {

    @Bean(name = "session-cmr")
    public ChatMemoryRepository chatMemoryRepository(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        if (vectorStore instanceof VectorStoreConfiguration.EmptyVectorStore) {
            return new InMemoryChatMemoryRepository();
        } else {
            return JdbcChatMemoryRepository.builder().jdbcTemplate(jdbcTemplate).build();
        }
    }
}
