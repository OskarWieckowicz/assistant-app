package com.assistant_app.assistant_app.chat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ChatClientConfigTest {

    @Test
    void streamingChatExposesProviderToolsAndReturnsToolResultToModelWithEmptyRagContext() {
        ChatModel model = mock(ChatModel.class);
        VectorStore vectorStore = mock(VectorStore.class);
        given(model.getOptions()).willReturn(ToolCallingChatOptions.builder().build());
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        var countryTool = FunctionToolCallback.builder("get_country", (CountryRequest request) -> {
            assertThat(request.countryName()).isEqualTo("Poland");
            return "{\"capital\":\"Warsaw\",\"region\":\"Europe\",\"population\":12345678}";
        }).description("Look up country facts").inputType(CountryRequest.class).build();
        ToolCallbackProvider provider = ToolCallbackProvider.from(countryTool);

        given(model.stream(any(Prompt.class))).willReturn(
                Flux.just(new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "call-1", "function", "get_country", "{\"countryName\":\"Poland\"}")))
                        .build())))),
                Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("Warsaw"))))));

        ChatClient client = new ChatClientConfig().chatClient(ChatClient.builder(model), vectorStore, provider);
        String answer = new ChatService(client).sendMessage("What is the capital of Poland?")
                .collectList().map(chunks -> String.join("", chunks)).block(Duration.ofSeconds(10));

        assertThat(answer).isEqualTo("Warsaw");
        ArgumentCaptor<Prompt> prompts = ArgumentCaptor.forClass(Prompt.class);
        verify(model, times(2)).stream(prompts.capture());
        Prompt initialPrompt = prompts.getAllValues().getFirst();
        assertThat(initialPrompt.getOptions()).isInstanceOf(ToolCallingChatOptions.class);
        assertThat(((ToolCallingChatOptions) initialPrompt.getOptions()).getToolCallbacks())
                .extracting(callback -> callback.getToolDefinition().name()).contains("get_country");
        assertThat(prompts.getAllValues().getLast().getInstructions())
                .filteredOn(message -> message instanceof ToolResponseMessage)
                .singleElement().satisfies(message -> {
                    var responses = ((ToolResponseMessage) message).getResponses();
                    assertThat(responses).singleElement().satisfies(response -> {
                        assertThat(response.name()).isEqualTo("get_country");
                        assertThat(response.responseData()).contains("Warsaw", "12345678");
                    });
                });
    }

    record CountryRequest(String countryName) {
    }
}
