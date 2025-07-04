package org.tanzu.mcpclient.util;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for detecting and getting information about GenAI services.
 */
@Service
public class GenAIService {
    private static final Logger logger = LoggerFactory.getLogger(GenAIService.class);

    // Constants
    public static final String MCP_SERVICE_URL = "mcpServiceURL";

    public static final String CHAT_MODEL = "spring.ai.openai.chat.options.model";
    public static final String EMBEDDING_MODEL = "spring.ai.openai.embedding.options.model";

    private final Environment environment;
    private final CfEnv cfEnv;

    public GenAIService(Environment environment) {
        this.environment = environment;
        this.cfEnv = new CfEnv();
    }

    public boolean isEmbeddingModelAvailable() {
        return environment.getProperty(EMBEDDING_MODEL) != null;
    }

    public String getEmbeddingModelName() {
        return Objects.requireNonNullElse(environment.getProperty(EMBEDDING_MODEL), "");
    }

    public String getChatModelName() {
        return Objects.requireNonNullElse(environment.getProperty(CHAT_MODEL), "");
    }

    public List<String> getMcpServiceNames() {
        try {
            return cfEnv.findAllServices().stream()
                    .filter(this::hasMcpServiceUrl)
                    .map(CfService::getName)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Error getting MCP service names: {}", e.getMessage());
            return List.of();
        }
    }

    public List<String> getMcpServiceUrls() {
        try {
            return cfEnv.findAllServices().stream()
                    .filter(this::hasMcpServiceUrl)
                    .map(service -> service.getCredentials().getString(MCP_SERVICE_URL))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Error getting MCP service URLs: {}", e.getMessage());
            return List.of();
        }
    }

    public boolean hasMcpServiceUrl(CfService service) {
        CfCredentials credentials = service.getCredentials();
        return credentials != null && credentials.getString(MCP_SERVICE_URL) != null;
    }
}