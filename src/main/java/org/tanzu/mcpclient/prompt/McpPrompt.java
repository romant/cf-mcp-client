package org.tanzu.mcpclient.prompt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents an MCP prompt with metadata and arguments.
 * This is the internal representation used by the application to store
 * and manage prompts discovered from MCP servers.
 *
 * @param serverId The ID of the MCP server that provides this prompt
 * @param serverName The display name of the MCP server from serverInfo
 * @param name The name of the prompt
 * @param description Optional description of what this prompt does
 * @param arguments List of arguments that this prompt accepts
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpPrompt(
        @JsonProperty("serverId") String serverId,
        @JsonProperty("serverName") String serverName,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("arguments") List<PromptArgument> arguments
) {

    /**
     * Returns the unique identifier for this prompt (serverId:name).
     */
    public String getId() {
        return serverId + ":" + name;
    }

    /**
     * Checks if this prompt requires any arguments.
     */
    public boolean hasArguments() {
        return arguments != null && !arguments.isEmpty();
    }

    /**
     * Returns the count of required arguments.
     */
    public long getRequiredArgumentCount() {
        if (arguments == null) {
            return 0;
        }
        return arguments.stream().filter(PromptArgument::required).count();
    }

    /**
     * Checks if this prompt has any required arguments.
     */
    public boolean hasRequiredArguments() {
        return getRequiredArgumentCount() > 0;
    }

    /**
     * Returns the display name for the server (serverName if available, otherwise serverId).
     */
    public String getServerDisplayName() {
        return serverName != null && !serverName.trim().isEmpty() ? serverName : serverId;
    }
}