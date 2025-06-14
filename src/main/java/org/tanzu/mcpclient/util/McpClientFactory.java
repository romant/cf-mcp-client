package org.tanzu.mcpclient.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Utility factory for creating MCP clients with consistent configuration.
 * This factory centralizes the MCP client creation logic to ensure
 * all parts of the application use the same client configuration.
 */
@Component
public class McpClientFactory {

    private final SSLContext sslContext;

    public McpClientFactory(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    /**
     * Creates a new MCP synchronous client for the specified server URL.
     * The client is configured with appropriate timeouts and SSL context.
     */
    public McpSyncClient createMcpSyncClient(String serverUrl) {
        HttpClient.Builder clientBuilder = createHttpClientBuilder();

        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(serverUrl)
                .clientBuilder(clientBuilder)
                .objectMapper(new ObjectMapper())
                .build();

        return McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Creates an HTTP client builder with consistent configuration.
     */
    private HttpClient.Builder createHttpClientBuilder() {
        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(30));
    }

    /**
     * Static method for creating MCP clients when dependency injection isn't available.
     * This is useful for testing or special cases where the factory isn't available.
     */
    public static McpSyncClient createMcpSyncClient(String serverUrl, SSLContext sslContext) {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(30));

        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(serverUrl)
                .clientBuilder(clientBuilder)
                .objectMapper(new ObjectMapper())
                .build();

        return McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .build();
    }
}