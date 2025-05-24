package org.tanzu.mcpclient.chat;

import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * Event published when chat configuration values are available or change.
 */
public class ChatConfigurationEvent extends ApplicationEvent {
    private final String chatModel;
    private final List<String> agentServices;

    public ChatConfigurationEvent(Object source, String chatModel, List<String> agentServices) {
        super(source);
        this.chatModel = chatModel;
        this.agentServices = agentServices;
    }

    public String getChatModel() {
        return chatModel;
    }

    public List<String> getAgentServices() {
        return agentServices;
    }
}