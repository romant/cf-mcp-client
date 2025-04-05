package org.tanzu.mcpclient.chat;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Configuration
public class ChatConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ChatConfiguration.class);

    private static final String MCP_SERVICE_URL = "mcpServiceURL";
    private final List<String> agentServices = new ArrayList<>();
    private final List<String> mcpServiceURLs = new ArrayList<>();

    public ChatConfiguration() {
        CfEnv cfEnv = new CfEnv();
        List<CfService> cfServices = cfEnv.findAllServices();

        for (CfService cfService : cfServices) {
            CfCredentials cfCredentials = cfService.getCredentials();
            String mcpServiceUrl = cfCredentials.getString(MCP_SERVICE_URL);
            if (mcpServiceUrl != null) {
                mcpServiceURLs.add(mcpServiceUrl);
                logger.info("Bound to MCP Service: {}", mcpServiceUrl);
                agentServices.add(cfService.getName());
            }
        }
    }

    public List<String> getAgentServices() {
        return agentServices;
    }

    public List<String> getMcpServiceURLs() {
        return mcpServiceURLs;
    }

    @Bean
    public String getChatModel() {
        CfEnv cfEnv = new CfEnv();

        return cfEnv.findServicesByLabel("genai").stream()
                .filter(this::isChatService)
                .findFirst()
                .map(CfService::getName)
                .orElse("");
    }

    private boolean isChatService(CfService service) {
        return service.getCredentials() != null &&
                service.getCredentials().getString("model_capabilities") != null &&
                service.getCredentials().getString("model_capabilities").contains("chat");
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
