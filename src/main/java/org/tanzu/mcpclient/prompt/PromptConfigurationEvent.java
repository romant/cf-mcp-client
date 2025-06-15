package org.tanzu.mcpclient.prompt;

import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.Map;

/**
 * Event published when prompt discovery is completed or prompt configuration changes.
 */
public class PromptConfigurationEvent extends ApplicationEvent {
    private final int totalPrompts;
    private final int serversWithPrompts;
    private final boolean available;
    private final Map<String, List<McpPrompt>> promptsByServer;

    public PromptConfigurationEvent(Object source,
                                    int totalPrompts,
                                    int serversWithPrompts,
                                    boolean available,
                                    Map<String, List<McpPrompt>> promptsByServer) {
        super(source);
        this.totalPrompts = totalPrompts;
        this.serversWithPrompts = serversWithPrompts;
        this.available = available;
        this.promptsByServer = Map.copyOf(promptsByServer); // Defensive copy
    }

    public int getTotalPrompts() { return totalPrompts; }
    public int getServersWithPrompts() { return serversWithPrompts; }
    public boolean isAvailable() { return available; }
    public Map<String, List<McpPrompt>> getPromptsByServer() { return promptsByServer; }
}