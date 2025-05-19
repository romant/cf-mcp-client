package org.tanzu.mcpclient.util;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for detecting and getting information about GenAI services.
 */
@Service
public class GenAIService {
    private static final Logger logger = LoggerFactory.getLogger(GenAIService.class);

    // Constants
    public static final String GENAI_LABEL = "genai";
    public static final String MODEL_CAPABILITIES = "model_capabilities";
    public static final String MODEL_NAME = "model_name";
    public static final String EMBEDDING_CAPABILITY = "embedding";
    public static final String CHAT_CAPABILITY = "chat";
    public static final String MCP_SERVICE_URL = "mcpServiceURL";

    private final CfEnv cfEnv;

    public GenAIService() {
        this.cfEnv = new CfEnv();
    }

    public boolean isEmbeddingModelAvailable() {
        try {
            return cfEnv.findServicesByLabel(GENAI_LABEL).stream()
                    .anyMatch(service -> hasCapability(service, EMBEDDING_CAPABILITY));
        } catch (Exception e) {
            logger.warn("Error checking for embedding model availability: {}", e.getMessage());
            return false;
        }
    }

    public String getEmbeddingModelName() {
        try {
            return cfEnv.findServicesByLabel(GENAI_LABEL).stream()
                    .filter(service -> hasCapability(service, EMBEDDING_CAPABILITY))
                    .findFirst()
                    .map(this::getModelName)
                    .orElse("");
        } catch (Exception e) {
            logger.warn("Error getting embedding model name: {}", e.getMessage());
            return "";
        }
    }

    public String getChatModelName() {
        try {
            return cfEnv.findServicesByLabel(GENAI_LABEL).stream()
                    .filter(service -> hasCapability(service, CHAT_CAPABILITY))
                    .findFirst()
                    .map(this::getModelName)
                    .orElse("");
        } catch (Exception e) {
            logger.warn("Error getting chat model name: {}", e.getMessage());
            return "";
        }
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

    public boolean hasCapability(CfService service, String capability) {
        return Optional.ofNullable(service.getCredentials())
                .map(creds -> creds.getString(MODEL_CAPABILITIES))
                .filter(capabilities -> capabilities.contains(capability))
                .isPresent();
    }

    public boolean hasMcpServiceUrl(CfService service) {
        CfCredentials credentials = service.getCredentials();
        return credentials != null && credentials.getString(MCP_SERVICE_URL) != null;
    }

    public String getModelName(CfService service) {
        CfCredentials credentials = service.getCredentials();
        String modelName = credentials != null ? credentials.getString(MODEL_NAME) : null;
        return modelName != null ? modelName : service.getName();
    }
}