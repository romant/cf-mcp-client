package org.tanzu.mcpclient.chat;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Configuration
public class ChatConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ChatConfiguration.class);

    private static final String MCP_SERVICE_URL = "mcpServiceURL";
    private static final String MODEL_CAPABILITIES = "model_capabilities";
    private static final String MODEL_NAME = "model_name";
    private static final String CHAT_CAPABILITY = "chat";
    private static final String GENAI_LABEL = "genai";

    private final String chatModel;
    private final List<String> agentServices = new ArrayList<>();
    private final List<String> mcpServiceURLs = new ArrayList<>();

    public ChatConfiguration() {
        CfEnv cfEnv = new CfEnv();
        List<CfService> cfServices = cfEnv.findAllServices();

        for (CfService cfService : cfServices) {
            CfCredentials cfCredentials = cfService.getCredentials();
            String mcpServiceUrl = cfCredentials.getString(MCP_SERVICE_URL);
            if (mcpServiceUrl != null) {
                mcpServiceURLs.add(mcpServiceUrl);
                logger.info("Bound to MCP Service: {}", mcpServiceUrl);
                agentServices.add(cfService.getName());
            }
        }

        chatModel = cfEnv.findServicesByLabel(GENAI_LABEL).stream()
                .filter(this::isChatService)
                .findFirst()
                .map(cfService -> {
                    CfCredentials credentials = cfService.getCredentials();
                    String modelName = credentials != null ? credentials.getString(MODEL_NAME) : null;
                    return modelName != null ? modelName : cfService.getName();
                })
                .orElse("");
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

    private boolean isChatService(CfService service) {
        return Optional.ofNullable(service.getCredentials())
                .map(creds -> creds.getString(MODEL_CAPABILITIES))
                .filter(capabilities -> capabilities.contains(CHAT_CAPABILITY))
                .isPresent();
    }
}
