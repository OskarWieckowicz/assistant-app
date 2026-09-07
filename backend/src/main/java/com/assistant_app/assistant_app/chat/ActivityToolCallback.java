package com.assistant_app.assistant_app.chat;

import java.util.UUID;
import java.util.List;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

final class ActivityToolCallback implements ToolCallback {
    static final String TOOL_CALLS = ActivityToolCallback.class.getName() + ".events";
    private final ToolCallback delegate;

    ActivityToolCallback(ToolCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String input) {
        return delegate.call(input);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String call(String input, ToolContext context) {
        Object history = context == null ? null : context.getContext().get(TOOL_CALLS);
        if (!(history instanceof List<?>)) {
            return delegate.call(input, context);
        }
        List<ChatEvent> toolCalls = (List<ChatEvent>) history;
        String callId = UUID.randomUUID().toString();
        String name = getToolDefinition().name();
        try {
            String result = delegate.call(input, context);
            toolCalls.add(ChatEvent.tool("tool_finished", callId, name, input, result));
            return result;
        } catch (RuntimeException exception) {
            toolCalls.add(ChatEvent.tool("tool_failed", callId, name, input, "The tool could not complete the request."));
            throw exception;
        }
    }
}
