package org.tanzu.mcpclient.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class ChatConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ChatConfiguration.class);

    @Bean
    ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    private static final String VCAP_SERVICES = "VCAP_SERVICES";
    private static final String USER_PROVIDED_SERVICE = "user-provided";
    private static final String MCP_SERVICE_URL = "mcpServiceURL";

    @Bean
    public List<String> mcpServiceURLs() {
        List<String> mcpServiceURLs = new ArrayList<>();

        String vcapServices = System.getenv(VCAP_SERVICES);
        if (vcapServices == null || vcapServices.trim().isEmpty()) {
            return mcpServiceURLs;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(vcapServices);

            JsonNode userProvidedServicesNode = root.get(USER_PROVIDED_SERVICE);
            if (userProvidedServicesNode == null || !userProvidedServicesNode.isArray()) {
                logger.info("No services found of type: " + USER_PROVIDED_SERVICE);
                return mcpServiceURLs;
            }

            // Search through service instances
            for (JsonNode serviceNode : userProvidedServicesNode) {
                JsonNode credentials = serviceNode.get("credentials");
                Map<String, String> map = mapper.convertValue(credentials, Map.class);
                logger.info("Found credentials:\n" + map);
                String mcpServiceURL = map.get(MCP_SERVICE_URL);
                if (mcpServiceURL != null) {
                    mcpServiceURLs.add(mcpServiceURL);
                    logger.info("Bound to MCP Service: " + mcpServiceURL);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing VCAP_SERVICES", e);
        }

        return mcpServiceURLs;
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.addAllowedOrigin("*");
        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");
        // corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE");
            }
        };
    }

    @Bean
    public SSLContext sslContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCertificates = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCertificates, new java.security.SecureRandom());
        return sslContext;
    }
}
