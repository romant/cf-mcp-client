package org.tanzu.mcpclient.document;

import org.springframework.context.ApplicationEvent;

/**
 * Event published when document configuration values are available or change.
 */
public class DocumentConfigurationEvent extends ApplicationEvent {
    private final String embeddingModel;
    private final String vectorStoreName;

    public DocumentConfigurationEvent(Object source, String embeddingModel, String vectorStoreName) {
        super(source);
        this.embeddingModel = embeddingModel;
        this.vectorStoreName = vectorStoreName;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public String getVectorStoreName() {
        return vectorStoreName;
    }
}