package org.tanzu.mcpclient.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.tanzu.mcpclient.chat.ChatConfiguration;
import org.tanzu.mcpclient.document.DocumentConfiguration;
import org.tanzu.mcpclient.memgpt.MemGPTConfiguration;

import java.util.Map;

@RestController
public class MetricsController {

    private final MemGPTConfiguration memGPTConfiguration;
    private final ChatConfiguration chatConfiguration;
    private final DocumentConfiguration documentConfiguration;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);
    private static final String METRICS_PATH = "/metrics/";

    public MetricsController(MemGPTConfiguration memGPTConfiguration, ChatConfiguration chatConfiguration,
                             DocumentConfiguration documentConfiguration) {
        this.memGPTConfiguration = memGPTConfiguration;
        this.chatConfiguration = chatConfiguration;
        this.documentConfiguration = documentConfiguration;
    }

    @GetMapping("/memory/metrics/{conversationId}")
    public Metrics getMetrics(@PathVariable String conversationId) {

        Integer contextSize = null;
        String humanBlockValue = "";
        String personaBlockValue = "";

        if (memGPTConfiguration.memGPTUrl().isPresent()) {
            String targetURL = UriComponentsBuilder
                    .fromUriString(memGPTConfiguration.memGPTUrl().get())
                    .path(METRICS_PATH)
                    .path(conversationId)
                    .build()
                    .toString();
            try {
                ResponseEntity<Map<String,Object>> responseEntity = restTemplate.exchange(targetURL, HttpMethod.GET,
                        null, new ParameterizedTypeReference<>() {});
                Map<String, Object> entity = responseEntity.getBody();
                contextSize = (Integer) entity.get("contextSize");
                humanBlockValue = (String) entity.get("humanBlockValue");
                personaBlockValue = (String) entity.get("personaBlockValue");
            }
            catch (RestClientException ex) {
                logger.error("Error retrieving memory metrics: {}", ex.getMessage());
            }
        }

        return new Metrics(chatConfiguration.getChatModel(), documentConfiguration.getEmbeddingModel(),
                documentConfiguration.getVectorDatabase(), chatConfiguration.getAgentServices().toArray(new String[0]),
                memGPTConfiguration.getMemoryService(), contextSize, humanBlockValue, personaBlockValue);
    }

    public record Metrics(String chatModel, String embeddingModel, String vectorDatabase, String[] agents,
                          String memoryService, Integer contextSize, String humanBlockValue, String personaBlockValue){
    }
}


