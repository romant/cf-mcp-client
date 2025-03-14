package org.tanzu.mcpclient.binding;

import org.springframework.cloud.bindings.Binding;
import org.springframework.cloud.bindings.Bindings;
import org.springframework.cloud.bindings.boot.BindingsPropertiesProcessor;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

import java.util.Arrays;
import java.util.Map;

/**
 * An implementation of {@link BindingsPropertiesProcessor} that detects {@link Binding}s
 * of type: {@value TYPE}.
 *
 * @author Stuart Charlton
 */
public class TanzuBindingsPropertiesProcessor implements BindingsPropertiesProcessor {

    /**
     * The {@link Binding} type that this processor is interested in: {@value}.
     **/
    public static final String TYPE = "genai";

    @Override
    public void process(@NonNull Environment environment, Bindings bindings, @NonNull Map<String, Object> properties) {
        bindings.filterBindings(TYPE).forEach(binding -> {
            if (binding.getSecret().get("model-capabilities") != null) {
                String[] capabilities = binding.getSecret().get("model-capabilities").trim().split("\\s*,\\s*");
                if (Arrays.asList(capabilities).contains("chat")) {
                    properties.put("spring.ai.openai.chat.api-key", binding.getSecret().get("api-key"));
                    properties.put("spring.ai.openai.chat.base-url", binding.getSecret().get("uri"));
                    properties.put("spring.ai.openai.chat.options.model", binding.getSecret().get("model-name"));
                }
                if (Arrays.asList(capabilities).contains("embedding")) {
                    properties.put("spring.ai.openai.embedding.api-key", binding.getSecret().get("api-key"));
                    properties.put("spring.ai.openai.embedding.base-url", binding.getSecret().get("uri"));
                    properties.put("spring.ai.openai.embedding.options.model", binding.getSecret().get("model-name"));
                }
            }
        });
    }
}