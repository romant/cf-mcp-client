package org.tanzu.mcpclient.chat;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat")
    public CfChatResponse chat(@RequestParam("chat") String chat,
                               @RequestParam(value = "documentId", required = false) Optional<String> documentId,
                               HttpServletRequest request) {
        // Get conversationId from the session ID (JSESSIONID)
        String conversationId = request.getSession().getId();

        String response = chatService.chat(chat, conversationId, documentId);
        return new CfChatResponse(response);
    }

    public record CfChatResponse(String message) {
    }
}
