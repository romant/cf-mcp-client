package org.tanzu.mcpclient.metrics;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tanzu.mcpclient.chat.ChatConfiguration;
import org.tanzu.mcpclient.document.DocumentConfiguration;

@RestController
public class MetricsController {

    private final ChatConfiguration chatConfiguration;
    private final DocumentConfiguration documentConfiguration;

    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);

    public MetricsController(ChatConfiguration chatConfiguration, DocumentConfiguration documentConfiguration) {
        this.chatConfiguration = chatConfiguration;
        this.documentConfiguration = documentConfiguration;
    }

    @GetMapping("/metrics")
    public Metrics getMetrics(HttpServletRequest request) {
        String conversationId = request.getSession().getId();

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


