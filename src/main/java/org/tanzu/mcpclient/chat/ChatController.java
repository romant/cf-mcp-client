package org.tanzu.mcpclient.chat;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat")
    public CfChatResponse chat(@RequestParam("chat") String chat) {
        String response = chatService.chat(chat);
        return new CfChatResponse(response);
    }

    record CfChatResponse(String message) {
    }
}
