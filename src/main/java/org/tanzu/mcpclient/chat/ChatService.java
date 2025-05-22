package org.tanzu.mcpclient.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.tanzu.mcpclient.document.DocumentService;
import reactor.core.publisher.Flux;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final List<String> mcpServiceURLs;
    private final SSLContext sslContext;

    @Value("classpath:/prompts/system-prompt.st")
    private Resource systemChatPrompt;

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    public ChatService(ChatClient.Builder chatClientBuilder, BaseChatMemoryAdvisor memoryAdvisor,
                       List<String> mcpServiceURLs, VectorStore vectorStore, SSLContext sslContext) {
        chatClientBuilder = chatClientBuilder.defaultAdvisors(memoryAdvisor, new SimpleLoggerAdvisor());
        this.chatClient = chatClientBuilder.build();

        this.mcpServiceURLs = mcpServiceURLs;
        this.vectorStore = vectorStore;
        this.sslContext = sslContext;
    }

    public Flux<String> chatStream(String chat, String conversationId, Optional<String> documentId) {
        try (Stream<McpSyncClient> mcpSyncClients = createAndInitializeMcpClients()) {
            ToolCallbackProvider[] toolCallbackProviders = mcpSyncClients
                    .map(SyncMcpToolCallbackProvider::new)
                    .toArray(ToolCallbackProvider[]::new);

            logger.info("CHAT STREAM REQUEST: conversationID = {}", conversationId);
            return buildAndExecuteStreamChatRequest(chat, conversationId, documentId, toolCallbackProviders);
        }
    }

    private Stream<McpSyncClient> createAndInitializeMcpClients() {
        HttpClient.Builder clientBuilder = createHttpClientBuilder();

        return mcpServiceURLs.stream()
                .map(url -> createMcpSyncClient(clientBuilder, url))
                .peek(McpSyncClient::initialize);
    }

    private HttpClient.Builder createHttpClientBuilder() {
        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(30));
    }

    private McpSyncClient createMcpSyncClient(HttpClient.Builder clientBuilder, String url) {
        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(url).
                clientBuilder(clientBuilder).
                objectMapper(new ObjectMapper()).
                build();
        return McpClient.sync(transport).requestTimeout(Duration.ofSeconds(30)).build();
    }

    private Flux<String> buildAndExecuteStreamChatRequest(String chat, String conversationId, Optional<String> documentId,
                                                          ToolCallbackProvider[] toolCallbackProviders) {

        ChatClient.ChatClientRequestSpec spec = chatClient.
                prompt().
                user(chat).
                system(systemChatPrompt).
                toolCallbacks(toolCallbackProviders);

        if (documentId.isPresent()) {
            spec = addDocumentSearchCapabilities(spec, documentId.get());
        }

        spec = spec.advisors(a -> a.param(CONVERSATION_ID, conversationId));

        return spec.stream().content()
                .filter(Objects::nonNull);
    }

    private ChatClient.ChatClientRequestSpec addDocumentSearchCapabilities(
            ChatClient.ChatClientRequestSpec spec,
            String documentId) {

        Advisor questionAnswerAdvisor = new QuestionAnswerAdvisor(this.vectorStore);

        String filterExpression = DocumentService.DOCUMENT_ID + " == '" + documentId + "'";

        return spec.advisors(questionAnswerAdvisor)
                .advisors(advisorSpec ->
                        advisorSpec.param(QuestionAnswerAdvisor.FILTER_EXPRESSION, filterExpression));
    }
}