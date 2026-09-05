package com.assistant_app.assistant_app.chat;

import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;
import java.util.List;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

// testing the embedding model is working
@RestController
@RequestMapping("/api/embedding")
@RequiredArgsConstructor 
public class EmbeddingController {

    private final EmbeddingModel embeddingModel;


    @GetMapping()
    public Map<String, EmbeddingResponse> embed(@RequestParam( defaultValue = "Tell me a joke") String message) {
        EmbeddingResponse embeddingResponse = this.embeddingModel.embedForResponse(List.of(message));
        return Map.of("embedding", embeddingResponse);
    }
}
