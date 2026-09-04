package com.assistant_app.assistant_app.chat;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping
    public String chat(@RequestBody MessageRequest request) {
        return chatService.sendMessage(request.message());
    }
    
}
