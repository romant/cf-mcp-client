package org.tanzu.mcpclient.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.tanzu.mcpclient.metrics.Agent;
import org.tanzu.mcpclient.util.GenAIService;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class ChatConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ChatConfiguration.class);

    private final String chatModel;
    private final List<String> agentServices;
    private final List<String> allMcpServiceURLs;
    private final List<Agent> agentsWithHealth;
    private final List<String> healthyMcpServiceURLs;
    private final ApplicationEventPublisher eventPublisher;
    private final SSLContext sslContext;

    // Map to store server names by URL for use by other services
    private final Map<String, String> serverNamesByUrl = new ConcurrentHashMap<>();

    public ChatConfiguration(GenAIService genAIService, ApplicationEventPublisher eventPublisher, SSLContext sslContext) {
        this.chatModel = genAIService.getChatModelName();
        this.agentServices = genAIService.getMcpServiceNames();
        this.allMcpServiceURLs = genAIService.getMcpServiceUrls();
        this.eventPublisher = eventPublisher;
        this.sslContext = sslContext;
        this.agentsWithHealth = new ArrayList<>();
        this.healthyMcpServiceURLs = new ArrayList<>();

        if (!allMcpServiceURLs.isEmpty()) {
            logger.info("Found MCP Services: {}", allMcpServiceURLs);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void publishConfigurationEvent() {
        // Test MCP server health during application startup
        testMcpServerHealth();

        logger.debug("Publishing ChatConfigurationEvent: chatModel={}, agentsWithHealth={}",
                chatModel, agentsWithHealth);
        eventPublisher.publishEvent(new ChatConfigurationEvent(this, chatModel, agentsWithHealth));
    }

    @Bean
    public List<String> mcpServiceURLs() {
        return healthyMcpServiceURLs; // Only return healthy MCP service URLs
    }

    @Bean
    public Map<String, String> serverNamesByUrl() {
        return serverNamesByUrl;
    }

    /**
     * Test the health of all configured MCP servers by attempting to initialize them.
     */
    private void testMcpServerHealth() {
        agentsWithHealth.clear();
        healthyMcpServiceURLs.clear();
        serverNamesByUrl.clear();

        if (agentServices.isEmpty() || allMcpServiceURLs.isEmpty()) {
            logger.debug("No MCP services configured for health checking");
            return;
        }

        for (int i = 0; i < agentServices.size() && i < allMcpServiceURLs.size(); i++) {
            String serviceName = agentServices.get(i);
            String serviceUrl = allMcpServiceURLs.get(i);

            Agent agent = testMcpServerHealthAndGetTools(serviceName, serviceUrl);
            agentsWithHealth.add(agent);

            // Only add healthy servers to the list used by ChatService
            if (agent.healthy()) {
                healthyMcpServiceURLs.add(serviceUrl);
            }
        }

        int healthyCount = healthyMcpServiceURLs.size();
        int totalCount = agentsWithHealth.size();

        logger.info("MCP Server health check completed. Healthy: {}, Unhealthy: {}",
                healthyCount, totalCount - healthyCount);

        if (healthyCount > 0) {
            logger.info("Healthy MCP servers that will be used for chat: {}", healthyMcpServiceURLs);
        }

        if (healthyCount < totalCount) {
            logger.warn("Some MCP servers are unhealthy and will not be used for chat operations");
        }
    }

    /**
     * Test the health of a single MCP server by attempting to initialize it and get its tools.
     */
    private Agent testMcpServerHealthAndGetTools(String serviceName, String serviceUrl) {
        logger.debug("Testing health of MCP server: {} at {}", serviceName, serviceUrl);

        List<Agent.Tool> tools = new ArrayList<>();
        String serverName = serviceName; // Default to service name

        try {
            HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(10)); // Shorter timeout for health checks

            HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(serviceUrl)
                    .clientBuilder(clientBuilder)
                    .objectMapper(new ObjectMapper())
                    .build();

            McpSyncClient client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(10)) // Shorter timeout for health checks
                    .build();

            // Attempt to initialize the client
            var initResult = client.initialize();

            // Get server name from initialization result
            if (initResult != null && initResult.serverInfo() != null && initResult.serverInfo().name() != null) {
                serverName = initResult.serverInfo().name();
                logger.debug("Retrieved server name '{}' for MCP server at {}", serverName, serviceUrl);
            } else {
                logger.debug("No server name available for MCP server at {}, using service name '{}'", serviceUrl, serviceName);
            }

            // Store the server name for use by other services
            serverNamesByUrl.put(serviceUrl, serverName);

            // If we get here, the server is healthy - now get the tools
            logger.debug("MCP server {} is healthy, fetching tools...", serviceName);

            try {
                var listToolsResult = client.listTools();
                if (listToolsResult != null && listToolsResult.tools() != null) {
                    tools = listToolsResult.tools().stream()
                            .map(tool -> new Agent.Tool(tool.name(), tool.description()))
                            .toList();
                    logger.debug("Found {} tools for MCP server {}: {}",
                            tools.size(), serviceName,
                            tools.stream().map(Agent.Tool::name).toList());
                }
            } catch (Exception e) {
                logger.warn("Failed to get tools for MCP server {} (server is healthy but tools unavailable): {}",
                        serviceName, e.getMessage());
            }

            // Clean up the test client
            client.closeGracefully();

            return new Agent(serviceName, serverName, true, tools);

        } catch (Exception e) {
            logger.warn("MCP server {} at {} is unhealthy: {}", serviceName, serviceUrl, e.getMessage());
            return new Agent(serviceName, serverName, false, List.of());
        }
    }
}