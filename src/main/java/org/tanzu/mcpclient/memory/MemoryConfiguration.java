package org.tanzu.mcpclient.memory;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tanzu.mcpclient.vectorstore.VectorStoreConfiguration;

@Configuration
public class MemoryConfiguration {

    @Bean
    public BaseChatMemoryAdvisor chatMemoryAdvisor(ChatMemoryRepository chatMemoryRepository, VectorStore vectorStore) {
        BaseChatMemoryAdvisor memoryAdvisor;
        if ( vectorStore instanceof VectorStoreConfiguration.EmptyVectorStore ) {
            ChatMemory chatMemory = MessageWindowChatMemory.builder()
                    .chatMemoryRepository(chatMemoryRepository)
                    .maxMessages(20)
                    .build();
            memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        }
        else {
            memoryAdvisor = VectorStoreChatMemoryAdvisor.builder(vectorStore).defaultTopK(10).build();
        }

        return memoryAdvisor;
    }
}
