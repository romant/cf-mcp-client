package org.tanzu.mcpclient.prompt;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.tanzu.mcpclient.chat.ChatConfigurationEvent;
import org.tanzu.mcpclient.util.McpClientFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service responsible for discovering and managing MCP prompts from connected servers.
 * This service automatically discovers prompts at startup and maintains them in memory
 * for efficient access during runtime.
 *
 * <p>The service listens for ChatConfigurationEvent to ensure MCP servers are available
 * before attempting prompt discovery. It handles multi-server environments by namespacing
 * prompts with their server IDs to prevent conflicts.</p>
 *
 * @author AI Assistant
 */
@Service
public class PromptDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(PromptDiscoveryService.class);

    private final List<String> mcpServiceURLs;
    private final McpClientFactory mcpClientFactory;
    private final Map<String, List<McpPrompt>> promptsByServer = new ConcurrentHashMap<>();
    private final Map<String, McpPrompt> promptsById = new ConcurrentHashMap<>();

    public PromptDiscoveryService(List<String> mcpServiceURLs, McpClientFactory mcpClientFactory) {
        this.mcpServiceURLs = mcpServiceURLs;
        this.mcpClientFactory = mcpClientFactory;
    }

    /**
     * Listens for ChatConfigurationEvent and triggers prompt discovery once MCP servers are ready.
     */
    @EventListener
    public void onChatConfigurationReady(ChatConfigurationEvent event) {
        if (!mcpServiceURLs.isEmpty()) {
            logger.info("Starting prompt discovery for {} MCP servers", mcpServiceURLs.size());
            discoverPrompts();
        } else {
            logger.debug("No MCP service URLs configured, skipping prompt discovery");
        }
    }

    /**
     * Discovers prompts from all configured MCP servers.
     * This method creates temporary clients for discovery to avoid resource conflicts.
     */
    private void discoverPrompts() {
        promptsByServer.clear();
        promptsById.clear();

        for (String mcpUrl : mcpServiceURLs) {
            try {
                discoverPromptsFromServer(mcpUrl);
            } catch (Exception e) {
                logger.warn("Failed to discover prompts from server {}: {}", mcpUrl, e.getMessage());
            }
        }

        logger.info("Prompt discovery completed. Total prompts: {}, Servers with prompts: {}",
                promptsById.size(), promptsByServer.size());
    }

    /**
     * Discovers prompts from a single MCP server.
     */
    private void discoverPromptsFromServer(String mcpUrl) {
        String serverId = generateServerId(mcpUrl);

        try (var mcpClient = createMcpClient(mcpUrl)) {
            mcpClient.initialize();

            var listPromptsResult = mcpClient.listPrompts();
            if (listPromptsResult != null && listPromptsResult.prompts() != null) {
                List<McpPrompt> serverPrompts = listPromptsResult.prompts().stream()
                        .map(prompt -> convertToMcpPrompt(serverId, prompt))
                        .collect(Collectors.toList());

                promptsByServer.put(serverId, serverPrompts);

                // Create unique IDs for global lookup
                serverPrompts.forEach(prompt -> {
                    String promptId = serverId + ":" + prompt.name();
                    promptsById.put(promptId, prompt);
                });

                logger.debug("Discovered {} prompts from server {} ({})",
                        serverPrompts.size(), serverId, mcpUrl);
            }
        } catch (Exception e) {
            logger.error("Error discovering prompts from server {} ({}): {}",
                    serverId, mcpUrl, e.getMessage(), e);
        }
    }

    /**
     * Creates a temporary MCP client for prompt discovery.
     */
    private McpSyncClient createMcpClient(String mcpUrl) {
        return mcpClientFactory.createMcpSyncClient(mcpUrl);
    }

    /**
     * Generates a server ID from the MCP URL for namespacing.
     */
    private String generateServerId(String mcpUrl) {
        try {
            var uri = java.net.URI.create(mcpUrl);
            String host = uri.getHost();
            int port = uri.getPort();
            return port != -1 ? host + ":" + port : host;
        } catch (Exception e) {
            // Fallback to URL hash if parsing fails
            return "server-" + Math.abs(mcpUrl.hashCode());
        }
    }

    /**
     * Converts an MCP schema prompt to our internal McpPrompt representation.
     */
    private McpPrompt convertToMcpPrompt(String serverId, McpSchema.Prompt prompt) {
        List<PromptArgument> arguments = Optional.ofNullable(prompt.arguments())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::convertToPromptArgument)
                .collect(Collectors.toList());

        return new McpPrompt(
                serverId,
                prompt.name(),
                prompt.description(),
                arguments
        );
    }

    /**
     * Converts an MCP schema prompt argument to our internal PromptArgument representation.
     */
    private PromptArgument convertToPromptArgument(McpSchema.PromptArgument arg) {
        return new PromptArgument(
                arg.name(),
                arg.description(),
                Optional.ofNullable(arg.required()).orElse(false),
                null, // Default value not provided in MCP schema
                null  // JSON schema not provided in MCP prompt arguments
        );
    }

    /**
     * Returns all discovered prompts from all servers.
     */
    public List<McpPrompt> getAllPrompts() {
        return new ArrayList<>(promptsById.values());
    }

    /**
     * Returns prompts grouped by server ID.
     */
    public Map<String, List<McpPrompt>> getPromptsByServer() {
        return new HashMap<>(promptsByServer);
    }

    /**
     * Finds a prompt by its unique ID (serverId:promptName).
     */
    public Optional<McpPrompt> findPromptById(String promptId) {
        return Optional.ofNullable(promptsById.get(promptId));
    }

    /**
     * Finds prompts by server ID.
     */
    public List<McpPrompt> findPromptsByServer(String serverId) {
        return promptsByServer.getOrDefault(serverId, Collections.emptyList());
    }

    /**
     * Returns the count of discovered prompts.
     */
    public int getPromptCount() {
        return promptsById.size();
    }

    /**
     * Returns the count of servers that have prompts.
     */
    public int getServerCount() {
        return promptsByServer.size();
    }

    /**
     * Checks if any prompts are available.
     */
    public boolean hasPrompts() {
        return !promptsById.isEmpty();
    }
}