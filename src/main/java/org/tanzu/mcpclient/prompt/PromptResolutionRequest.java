package org.tanzu.mcpclient.prompt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Request object for resolving a prompt with specific arguments.
 *
 * @param promptId The unique ID of the prompt to resolve (serverId:promptName)
 * @param arguments Map of argument names to values
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromptResolutionRequest(
        @JsonProperty("promptId") String promptId,
        @JsonProperty("arguments") Map<String, Object> arguments
) {

    /**
     * Extracts the server ID from the prompt ID.
     */
    public String getServerId() {
        if (promptId == null || !promptId.contains(":")) {
            return null;
        }
        return promptId.substring(0, promptId.indexOf(":"));
    }

    /**
     * Extracts the prompt name from the prompt ID.
     */
    public String getPromptName() {
        if (promptId == null || !promptId.contains(":")) {
            return promptId;
        }
        return promptId.substring(promptId.indexOf(":") + 1);
    }
}