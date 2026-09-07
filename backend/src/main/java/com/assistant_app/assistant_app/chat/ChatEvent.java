package com.assistant_app.assistant_app.chat;

public record ChatEvent(String type, String text, String callId, String name, String input) {
    public static ChatEvent text(String type, String text) {
        return new ChatEvent(type, text, null, null, null);
    }

    public static ChatEvent tool(String type, String callId, String name, String input, String text) {
        return new ChatEvent(type, text, callId, name, input);
    }
}
