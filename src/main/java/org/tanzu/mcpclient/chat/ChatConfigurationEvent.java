package org.tanzu.mcpclient.chat;

import org.springframework.context.ApplicationEvent;
import org.tanzu.mcpclient.metrics.Agent;

import java.util.List;

public class ChatConfigurationEvent extends ApplicationEvent {
    private final String chatModel;
    private final List<Agent> agentsWithHealth;

    public ChatConfigurationEvent(Object source, String chatModel, List<Agent> agentsWithHealth) {
        super(source);
        this.chatModel = chatModel;
        this.agentsWithHealth = agentsWithHealth;
    }

    public String getChatModel() {
        return chatModel;
    }

    public List<Agent> getAgentsWithHealth() {
        return agentsWithHealth;
    }
}