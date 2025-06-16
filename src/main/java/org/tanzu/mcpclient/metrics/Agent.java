package org.tanzu.mcpclient.metrics;

import java.util.List;

public record Agent(
        String name,
        String serverName,
        boolean healthy,
        List<Tool> tools
) {

    public record Tool(String name, String description) {
    }

    /**
     * Returns the display name for the agent (serverName if available, otherwise name).
     */
    public String getDisplayName() {
        return serverName != null && !serverName.trim().isEmpty() ? serverName : name;
    }
}