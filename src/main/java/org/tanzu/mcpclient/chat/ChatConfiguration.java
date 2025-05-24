package org.tanzu.mcpclient.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.tanzu.mcpclient.util.GenAIService;

import java.util.List;

@Configuration
public class ChatConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ChatConfiguration.class);

    private final String chatModel;
    private final List<String> agentServices;
    private final List<String> mcpServiceURLs;
    private final ApplicationEventPublisher eventPublisher;

    public ChatConfiguration(GenAIService genAIService, ApplicationEventPublisher eventPublisher) {
        this.chatModel = genAIService.getChatModelName();
        this.agentServices = genAIService.getMcpServiceNames();
        this.mcpServiceURLs = genAIService.getMcpServiceUrls();
        this.eventPublisher = eventPublisher;

        if (!mcpServiceURLs.isEmpty()) {
            logger.info("Bound to MCP Services: {}", mcpServiceURLs);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void publishConfigurationEvent() {
        logger.debug("Publishing ChatConfigurationEvent: chatModel={}, agentServices={}",
                chatModel, agentServices);
        eventPublisher.publishEvent(new ChatConfigurationEvent(this, chatModel, agentServices));
    }

    @Bean
    public List<String> mcpServiceURLs() {
        return mcpServiceURLs;
    }
}