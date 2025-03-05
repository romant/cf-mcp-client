package org.tanzu.mcpclient.chat;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.bindings.boot.BindingsPropertiesProcessor;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ChatAutoConfiguration {
    @Bean
    public BindingsPropertiesProcessor openAiBindingsPropertiesProcessor() {
        return new TanzuBindingsPropertiesProcessor();
    }
}
