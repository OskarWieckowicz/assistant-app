package com.assistant_app.assistant_app.chat;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatClient chatClient;

    public Flux<ChatEvent> sendMessage(String message) {
        return Flux.defer(() -> {
            var toolCalls = new CopyOnWriteArrayList<ChatEvent>();
            return chatClient.prompt()
                    .user(message)
                    .toolContext(Map.of(ActivityToolCallback.TOOL_CALLS, toolCalls))
                    .stream()
                    .chatResponse()
                    .concatMapIterable(this::modelEvents)
                    .concatWith(Flux.defer(() -> Flux.fromIterable(toolCalls)
                            .concatWithValues(ChatEvent.text("done", null))))
                    .onErrorResume(error -> Flux.fromIterable(toolCalls)
                            .concatWithValues(ChatEvent.text("error", "Could not complete the response.")));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private List<ChatEvent> modelEvents(ChatResponse response) {
        if (response.getResult() == null)
            return List.of();
        var events = new ArrayList<ChatEvent>();
        var output = response.getResult().getOutput();

        if (output.getMetadata().get("thinking") instanceof String thinking && !thinking.isEmpty()) {
            events.add(ChatEvent.text("thinking_delta", thinking));
        }
        String answer = output.getText();
        if (answer != null && !answer.isEmpty()) {
            events.add(ChatEvent.text("answer_delta", answer));
        }
        return events;
    }
}
