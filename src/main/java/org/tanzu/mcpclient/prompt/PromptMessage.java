package org.tanzu.mcpclient.prompt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a structured message within a resolved prompt.
 * This corresponds to the MCP PromptMessage structure.
 *
 * @param role The role of the message sender (user, assistant, etc.)
 * @param content The content of the message
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromptMessage(
        @JsonProperty("role") String role,
        @JsonProperty("content") String content
) {

    /**
     * Common message roles.
     */
    public static final class Roles {
        public static final String USER = "user";
        public static final String ASSISTANT = "assistant";
        public static final String SYSTEM = "system";

        private Roles() {}
    }
}