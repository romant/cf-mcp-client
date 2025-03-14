package org.tanzu.mcpclient.binding;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.bindings.boot.BindingsPropertiesProcessor;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class BindingAutoConfiguration {
    @Bean
    public BindingsPropertiesProcessor openAiBindingsPropertiesProcessor() {
        return new TanzuBindingsPropertiesProcessor();
    }
}
