package com.assistant_app.assistant_app.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatClient chatClient;


    public Flux<String> sendMessage(String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }

}
