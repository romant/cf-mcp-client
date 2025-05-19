package org.tanzu.mcpclient.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tanzu.mcpclient.util.GenAIService;

import java.util.List;

@Configuration
public class ChatConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ChatConfiguration.class);

    private final String chatModel;
    private final List<String> agentServices;
    private final List<String> mcpServiceURLs;

    public ChatConfiguration(GenAIService genAIService) {
        this.chatModel = genAIService.getChatModelName();
        this.agentServices = genAIService.getMcpServiceNames();
        this.mcpServiceURLs = genAIService.getMcpServiceUrls();

        if (!mcpServiceURLs.isEmpty()) {
            logger.info("Bound to MCP Services: {}", mcpServiceURLs);
        }
    }

    @Bean
    public List<String> mcpServiceURLs() {
        return mcpServiceURLs;
    }

    public List<String> getAgentServices() {
        return agentServices;
    }

    public String getChatModel() {
        return chatModel;
    }
}