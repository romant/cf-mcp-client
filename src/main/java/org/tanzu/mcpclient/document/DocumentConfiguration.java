package org.tanzu.mcpclient.document;

import java.util.Optional;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import jakarta.servlet.MultipartConfigElement;

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

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        
        // Set maximum file size
        factory.setMaxFileSize(DataSize.ofMegabytes(50));
        
        // Set maximum request size (total file size)
        factory.setMaxRequestSize(DataSize.ofMegabytes(50));
        
        // Set location for temporary files
        factory.setLocation("");
        
        return factory.createMultipartConfig();
    }
}
