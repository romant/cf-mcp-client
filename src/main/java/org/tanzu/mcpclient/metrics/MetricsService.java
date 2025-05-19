package org.tanzu.mcpclient.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tanzu.mcpclient.chat.ChatConfiguration;
import org.tanzu.mcpclient.document.DocumentConfiguration;

@Service
public class MetricsService {

    private final ChatConfiguration chatConfiguration;
    private final DocumentConfiguration documentConfiguration;

    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);

    public MetricsService(ChatConfiguration chatConfiguration, DocumentConfiguration documentConfiguration) {
        this.chatConfiguration = chatConfiguration;
        this.documentConfiguration = documentConfiguration;
    }

    public Metrics getMetrics(String conversationId) {
        logger.debug("Retrieving metrics for conversation: {}", conversationId);

        return new Metrics(
                conversationId,
                chatConfiguration.getChatModel(),
                documentConfiguration.getEmbeddingModel(),
                documentConfiguration.getVectorDatabase(),
                chatConfiguration.getAgentServices().toArray(new String[0])
        );
    }

    public record Metrics(
            String conversationId,
            String chatModel,
            String embeddingModel,
            String vectorDatabase,
            String[] agents
    ) {}
}