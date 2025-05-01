package org.tanzu.mcpclient.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.tanzu.mcpclient.document.DocumentService;
import org.tanzu.mcpclient.document.VectorStoreConfiguration;
import org.tanzu.mcpclient.memgpt.MemGPTChatMemory;
import org.tanzu.mcpclient.memgpt.MemGPTMessageChatMemoryAdvisor;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final List<String> mcpServiceURLs;
    private final SSLContext sslContext;
    private final ChatMemory chatMemory;

    @Value("classpath:/prompts/system-prompt.st")
    private Resource systemChatPrompt;

    private static final Logger logger = LoggerFactory.getLogger(VectorStoreConfiguration.class);

    public ChatService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, List<String> mcpServiceURLs,
                       VectorStore vectorStore, SSLContext sslContext) {

        if (!(chatMemory instanceof MemGPTChatMemory)) {
            chatClientBuilder = chatClientBuilder.
                    defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory), new SimpleLoggerAdvisor()).
                    defaultAdvisors(a -> a.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10));
        }
        this.chatClient = chatClientBuilder.build();
        this.mcpServiceURLs = mcpServiceURLs;
        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;
        this.sslContext = sslContext;
    }

    public String chat(String chat, String conversationId, Optional<String> documentId) {
        try (Stream<McpSyncClient> mcpSyncClients = createAndInitializeMcpClients()) {
            ToolCallbackProvider[] toolCallbackProviders = mcpSyncClients
                    .map(SyncMcpToolCallbackProvider::new)
                    .toArray(ToolCallbackProvider[]::new);

            logger.info("CHAT REQUEST: conversationID = {}", conversationId);
            return buildAndExecuteChatRequest(chat, conversationId, documentId, toolCallbackProviders);
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

    private String buildAndExecuteChatRequest(String chat, String conversationId, Optional<String> documentId,
                                              ToolCallbackProvider[] toolCallbackProviders) {

        ChatClient.ChatClientRequestSpec spec = chatClient.
                prompt().
                user(chat).
                system(systemChatPrompt).
                toolCallbacks(toolCallbackProviders);

        if (documentId.isPresent()) {
            spec = addDocumentSearchCapabilities(spec, documentId.get());
        }

        if ( this.chatMemory instanceof MemGPTChatMemory memGPTChatMemory) {
                spec = spec.advisors(new MemGPTMessageChatMemoryAdvisor(memGPTChatMemory, conversationId));
        }

        return spec.call().content();
    }

    private ChatClient.ChatClientRequestSpec addDocumentSearchCapabilities(
            ChatClient.ChatClientRequestSpec spec,
            String documentId) {

        Advisor questionAnswerAdvisor = new QuestionAnswerAdvisor(
                this.vectorStore, SearchRequest.builder().build());

        String filterExpression = DocumentService.DOCUMENT_ID + " == '" + documentId + "'";

        return spec.advisors(questionAnswerAdvisor)
                .advisors(advisorSpec ->
                        advisorSpec.param(QuestionAnswerAdvisor.FILTER_EXPRESSION, filterExpression));
    }
}
