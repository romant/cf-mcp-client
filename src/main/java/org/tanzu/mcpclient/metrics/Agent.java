package org.tanzu.mcpclient.metrics;

import java.util.List;

public record Agent(String name, boolean healthy, List<Tool> tools) {

    public record Tool(String name, String description) {
    }
}