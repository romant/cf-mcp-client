package org.tanzu.mcpclient.prompt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Represents the result of resolving a prompt with specific arguments.
 * Contains the final content that can be sent to the chat system.
 *
 * @param content The resolved prompt content as a string
 * @param messages List of structured messages (if the prompt returns multiple messages)
 * @param metadata Additional metadata about the resolution process
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResolvedPrompt(
        @JsonProperty("content") String content,
        @JsonProperty("messages") List<PromptMessage> messages,
        @JsonProperty("metadata") Map<String, Object> metadata
) {

    /**
     * Returns true if the resolved prompt contains structured messages.
     */
    public boolean hasMessages() {
        return messages != null && !messages.isEmpty();
    }

    /**
     * Returns the primary content to display.
     * If messages are present, returns the content of the first message,
     * otherwise returns the content field.
     */
    public String getPrimaryContent() {
        if (hasMessages()) {
            return messages.get(0).content();
        }
        return content;
    }
}
