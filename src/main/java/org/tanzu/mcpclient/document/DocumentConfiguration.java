package org.tanzu.mcpclient.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.tanzu.mcpclient.util.GenAIService;

@Configuration
public class DocumentConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DocumentConfiguration.class);

    private final String embeddingModel;
    private final String vectorDatabase;
    private final ApplicationEventPublisher eventPublisher;

    public DocumentConfiguration(VectorStore vectorStore, GenAIService genAIServiceUtil,
                                 ApplicationEventPublisher eventPublisher) {
        this.embeddingModel = genAIServiceUtil.getEmbeddingModelName();
        this.vectorDatabase = vectorStore.getName();
        this.eventPublisher = eventPublisher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void publishConfigurationEvent() {
        logger.debug("Publishing DocumentConfigurationEvent: embeddingModel={}, vectorDatabase={}",
                embeddingModel, vectorDatabase);
        eventPublisher.publishEvent(new DocumentConfigurationEvent(this, embeddingModel, vectorDatabase));
    }
}