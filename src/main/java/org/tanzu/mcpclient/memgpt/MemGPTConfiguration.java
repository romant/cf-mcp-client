package org.tanzu.mcpclient.memgpt;

import io.pivotal.cfenv.core.CfEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Optional;

@Configuration
public class MemGPTConfiguration {

    private static final String MEM_GPT = "memGPTUrl";
    private static final Logger logger = LoggerFactory.getLogger(MemGPTConfiguration.class);

    private Optional<String> memGPTUrl;
    private String memoryService = "";

    public MemGPTConfiguration() {
        CfEnv cfEnv = new CfEnv();

        cfEnv.findAllServices().stream()
                .filter(service -> service.getCredentials() != null &&
                        service.getCredentials().getString(MEM_GPT) != null)
                .findFirst()
                .ifPresent(service -> {
                    String memGPTUrl = service.getCredentials().getString(MEM_GPT);
                    this.memGPTUrl = Optional.of(memGPTUrl);
                    logger.info("Bound to MemGPT Service: {}", memGPTUrl);
                    memoryService = service.getName();
                });
    }

    @Bean
    public ChatMemory chatMemory(WebClient.Builder clientBuilder) {
        if (memGPTUrl.isPresent()) {
            return new MemGPTRestChatMemory(clientBuilder, memGPTUrl.get());
        } else {
            return new InMemoryChatMemory();
        }
    }

    public Optional<String> memGPTUrl() {
        return memGPTUrl;
    }

    public String getMemoryService() {
        return memoryService;
    }
}
