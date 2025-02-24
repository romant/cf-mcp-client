package org.tanzu.mcpclient.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Service
public class ChatService {

    private final ChatClient chatClient;

    @Value("classpath:/prompts/system-prompt.st")
    private Resource systemChatPrompt;

    public ChatService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, ToolCallbackProvider[] mcpClients) {
        this.chatClient = chatClientBuilder.
                defaultTools(mcpClients).
                defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory), new SimpleLoggerAdvisor()).
                defaultAdvisors(a -> a.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)).
                build();
    }

    public String chat(String chat) {
        return chatClient
                .prompt(chat)
                .call()
                .content();
    }
}
