package org.tanzu.mcpclient.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final List<String> mcpServiceURLs;
    private final SSLContext sslContext;

    @Value("classpath:/prompts/system-prompt.st")
    private Resource systemChatPrompt;

    public ChatService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, List<String> mcpServiceURLs,
                       SSLContext sslContext) {
        this.chatClient = chatClientBuilder.
                defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory), new SimpleLoggerAdvisor()).
                defaultAdvisors(a -> a.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)).
                build();
        this.mcpServiceURLs = mcpServiceURLs;
        this.sslContext = sslContext;
    }

    public String chat(String chat) {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(30));

        try ( // Open all McpSyncClients in try-with-resources
              Stream<McpSyncClient> mcpSyncClients = mcpServiceURLs.stream()
                      .map(url -> {
                          HttpClientSseClientTransport transport = new HttpClientSseClientTransport(
                                  clientBuilder, url, new ObjectMapper());
                          return McpClient.sync(transport).requestTimeout(Duration.ofSeconds(30)).build();
                      })
                      .peek(McpSyncClient::initialize)) {

            ToolCallbackProvider[] toolCallbackProviders = mcpSyncClients
                    .map(SyncMcpToolCallbackProvider::new)
                    .toArray(ToolCallbackProvider[]::new);

            return chatClient
                    .prompt(chat)
                    .tools(toolCallbackProviders)
                    .call()
                    .content();
        }
    }
}
