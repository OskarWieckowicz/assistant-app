package com.assistant_app.assistant_app.chat;

import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivityToolCallbackTest {
    @Test
    void reportsFailureToOnlyTheCurrentRequestAndPreservesContext() {
        ToolCallback delegate = mock(ToolCallback.class);
        when(delegate.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name("country").description("Country lookup").inputSchema("{}").build());
        var firstEvents = new ArrayList<ChatEvent>();
        var secondEvents = new ArrayList<ChatEvent>();
        var first = new ToolContext(Map.of(ActivityToolCallback.TOOL_CALLS,
                firstEvents));
        var second = new ToolContext(Map.of(ActivityToolCallback.TOOL_CALLS,
                secondEvents));
        when(delegate.call("{}", first)).thenAnswer(invocation -> {
            assertThat(firstEvents).isEmpty();
            throw new IllegalStateException("Unavailable");
        });
        when(delegate.call("{}", second)).thenReturn("Warsaw");
        var callback = new ActivityToolCallback(delegate);
        assertThatThrownBy(() -> callback.call("{}", first)).isInstanceOf(IllegalStateException.class);
        assertThat(callback.call("{}", second)).isEqualTo("Warsaw");
        assertThat(firstEvents).extracting(ChatEvent::type).containsExactly("tool_failed");
        assertThat(secondEvents).extracting(ChatEvent::type).containsExactly("tool_finished");
        assertThat(firstEvents.getFirst().input()).isEqualTo("{}");
        assertThat(firstEvents.getLast().text()).isEqualTo("The tool could not complete the request.");
        assertThat(secondEvents.getLast().text()).isEqualTo("Warsaw");
        assertThat(firstEvents.getFirst().callId()).isEqualTo(firstEvents.getLast().callId())
                .isNotEqualTo(secondEvents.getFirst().callId());
    }
}
