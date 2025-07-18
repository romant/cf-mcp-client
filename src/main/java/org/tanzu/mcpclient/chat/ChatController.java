package org.tanzu.mcpclient.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class ChatController {

    private final ChatService chatService;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam("chat") String chat,
                                 @RequestParam(value = "documentId", required = false) Optional<String> documentId,
                                 @RequestParam(value = "documentIds", required = false) Optional<List<String>> documentIds,
                                 HttpServletRequest request) {

        String conversationId = request.getSession().getId();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // Handle both single documentId (backward compatibility) and multiple documentIds
        List<String> finalDocumentIds = determineDocumentIds(documentId, documentIds);

        executor.execute(() -> {
            try {
                Flux<String> responseStream = chatService.chatStream(chat, conversationId, finalDocumentIds);

                responseStream
                        .filter(chunk -> chunk != null && !chunk.isEmpty())
                        .subscribe(
                                chunk -> {
                                    try {
                                        // Send as JSON to preserve exact content
                                        Map<String, String> payload = Map.of("content", chunk);
                                        String jsonData = objectMapper.writeValueAsString(payload);

                                        emitter.send(SseEmitter.event()
                                                .data(jsonData)
                                                .name("message"));
                                    } catch (IOException e) {
                                        emitter.completeWithError(e);
                                    }
                                },
                                emitter::completeWithError,
                                () -> {
                                    try {
                                        emitter.send(SseEmitter.event()
                                                .name("close")
                                                .data(""));
                                        emitter.complete();
                                    } catch (IOException e) {
                                        emitter.completeWithError(e);
                                    }
                                }
                        );

            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * Determines the final list of document IDs to use for the chat request.
     * Prioritizes documentIds parameter over documentId for backward compatibility.
     *
     * @param documentId Single document ID (legacy parameter)
     * @param documentIds Multiple document IDs (new parameter)
     * @return List of document IDs to use, empty if none provided
     */
    private List<String> determineDocumentIds(Optional<String> documentId, Optional<List<String>> documentIds) {
        // If documentIds is provided and not empty, use it
        if (documentIds.isPresent() && !documentIds.get().isEmpty()) {
            // Filter out null or empty strings
            return documentIds.get().stream()
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .toList();
        }

        // Fall back to single documentId for backward compatibility
        if (documentId.isPresent() && !documentId.get().trim().isEmpty()) {
            return List.of(documentId.get());
        }

        // No documents specified
        return List.of();
    }
}