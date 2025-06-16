package org.tanzu.mcpclient.chat;

import io.modelcontextprotocol.client.McpSyncClient;
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
import org.tanzu.mcpclient.util.McpClientFactory;
import reactor.core.publisher.Flux;

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
    private final McpClientFactory mcpClientFactory;

    @Value("classpath:/prompts/system-prompt.st")
    private Resource systemChatPrompt;

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    public ChatService(ChatClient.Builder chatClientBuilder, BaseChatMemoryAdvisor memoryAdvisor,
                       List<String> mcpServiceURLs, VectorStore vectorStore, McpClientFactory mcpClientFactory) {
        chatClientBuilder = chatClientBuilder.defaultAdvisors(memoryAdvisor, new SimpleLoggerAdvisor());
        this.chatClient = chatClientBuilder.build();

        this.mcpServiceURLs = mcpServiceURLs;
        this.vectorStore = vectorStore;
        this.mcpClientFactory = mcpClientFactory;
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
        return mcpServiceURLs.stream()
                .map(mcpClientFactory::createMcpSyncClient)
                .peek(McpSyncClient::initialize);
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