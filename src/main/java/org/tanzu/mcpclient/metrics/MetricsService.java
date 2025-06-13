package org.tanzu.mcpclient.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.tanzu.mcpclient.chat.ChatConfigurationEvent;
import org.tanzu.mcpclient.document.DocumentConfigurationEvent;
import org.tanzu.mcpclient.prompt.PromptDiscoveryService;

import java.util.List;

/**
 * Service that collects and provides platform metrics including models, agents, and prompts.
 * This service listens to various configuration events and maintains current state
 * for monitoring and status display purposes.
 */
@Service
public class MetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);

    private final PromptDiscoveryService promptDiscoveryService;

    private String chatModel = "";
    private List<Agent> agentsWithHealth = List.of();
    private String embeddingModel = "";
    private String vectorStoreName = "";

    public MetricsService(PromptDiscoveryService promptDiscoveryService) {
        this.promptDiscoveryService = promptDiscoveryService;
    }

    @EventListener
    public void handleChatConfigurationEvent(ChatConfigurationEvent event) {
        this.chatModel = event.getChatModel() != null ? event.getChatModel() : "";
        this.agentsWithHealth = event.getAgentsWithHealth() != null ? event.getAgentsWithHealth() : List.of();
        logger.debug("Updated chat metrics: model={}, agents={}", chatModel, agentsWithHealth.size());
    }

    @EventListener
    public void handleDocumentConfigurationEvent(DocumentConfigurationEvent event) {
        this.embeddingModel = event.getEmbeddingModel() != null ? event.getEmbeddingModel() : "";
        this.vectorStoreName = event.getVectorStoreName() != null ? event.getVectorStoreName() : "";
        logger.debug("Updated document metrics: embedding={}, vectorStore={}", embeddingModel, vectorStoreName);
    }

    /**
     * Retrieves comprehensive platform metrics including prompt information.
     */
    public Metrics getMetrics(String conversationId) {
        logger.debug("Retrieving metrics for conversation: {}", conversationId);

        // Get prompt metrics from the discovery service
        PromptMetrics promptMetrics = new PromptMetrics(
                promptDiscoveryService.getPromptCount(),
                promptDiscoveryService.getServerCount(),
                promptDiscoveryService.hasPrompts()
        );

        return new Metrics(
                conversationId,
                this.chatModel,
                this.embeddingModel,
                this.vectorStoreName,
                this.agentsWithHealth.toArray(new Agent[0]),
                promptMetrics
        );
    }

    /**
     * Enhanced metrics record that includes prompt information.
     */
    public record Metrics(
            String conversationId,
            String chatModel,
            String embeddingModel,
            String vectorStoreName,
            Agent[] agents,
            PromptMetrics prompts
    ) {}

    /**
     * Metrics specific to prompt discovery and availability.
     */
    public record PromptMetrics(
            int totalPrompts,
            int serversWithPrompts,
            boolean available
    ) {}
}