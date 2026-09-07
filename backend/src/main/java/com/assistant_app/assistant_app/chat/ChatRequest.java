package com.assistant_app.assistant_app.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank(message = "message must not be blank") String message) {
}
