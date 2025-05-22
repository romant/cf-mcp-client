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
import java.util.Map;
import java.util.Optional;
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
                                 HttpServletRequest request) {

        String conversationId = request.getSession().getId();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        executor.execute(() -> {
            try {
                Flux<String> responseStream = chatService.chatStream(chat, conversationId, documentId);

                responseStream
                        .filter(chunk -> chunk != null && !chunk.isEmpty())
                        .subscribe(
                                chunk -> {
                                    try {
                                        // Log what we're sending
                                        System.out.println("Backend sending chunk: [" + chunk + "] (length: " + chunk.length() + ")");

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
}