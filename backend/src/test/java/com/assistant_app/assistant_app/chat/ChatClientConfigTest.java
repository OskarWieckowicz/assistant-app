package com.assistant_app.assistant_app.chat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ChatClientConfigTest {

    @ParameterizedTest
    @ValueSource(strings = { "Berlin", "Test Capital" })
    void chainsCountryLookupIntoWeatherLookup(String capital) {
        // given
        ChatModel model = mock(ChatModel.class);
        VectorStore vectorStore = mock(VectorStore.class);
        given(model.getOptions()).willReturn(ToolCallingChatOptions.builder().build());
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        var calls = new ArrayList<String>();
        var json = new JsonMapper();
        var countryTool = FunctionToolCallback.builder("get_country", (CountryRequest request) -> {
            assertThat(request.countryName()).isEqualTo("Germany");
            calls.add("country");
            return new CapitalResult(capital);
        }).description("Return the country's capital").inputType(CountryRequest.class).build();
        var weatherTool = FunctionToolCallback.builder("get_weather", (WeatherRequest request) -> {
            assertThat(request.city()).isEqualTo(capital);
            calls.add("weather");
            return new TemperatureResult("16.8 C");
        }).description("Return the city's current temperature").inputType(WeatherRequest.class).build();

        given(model.stream(any(Prompt.class))).willAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            var toolResponses = prompt.getInstructions().stream()
                    .filter(ToolResponseMessage.class::isInstance)
                    .map(ToolResponseMessage.class::cast)
                    .flatMap(message -> message.getResponses().stream()).toList();
            AssistantMessage output;
            if (toolResponses.isEmpty()) {
                output = toolCall("country-call", "get_country", "{\"countryName\":\"Germany\"}");
            } else if (toolResponses.size() == 1) {
                var country = toolResponses.getFirst();
                assertThat(country.name()).isEqualTo("get_country");
                String returnedCapital = json.readValue(country.responseData(), CapitalResult.class).capital();
                assertThat(returnedCapital).isEqualTo(capital);
                output = toolCall("weather-call", "get_weather", json.writeValueAsString(new WeatherRequest(returnedCapital)));
            } else {
                assertThat(toolResponses).hasSize(2);
                var weather = toolResponses.getLast();
                assertThat(weather.name()).isEqualTo("get_weather");
                String temperature = json.readValue(weather.responseData(), TemperatureResult.class).temperature();
                assertThat(temperature).isEqualTo("16.8 C");
                output = new AssistantMessage(capital + ": " + temperature);
            }
            return Flux.just(new ChatResponse(List.of(new Generation(output))));
        });

        var client = new ChatClientConfig().chatClient(ChatClient.builder(model), vectorStore,
                ToolCallbackProvider.from(countryTool, weatherTool));

        // when
        var events = new ChatService(client)
                .sendMessage("What is the temperature of the capital of Germany currently?")
                .collectList().block(Duration.ofSeconds(10));

        // then
        assertThat(calls).containsExactly("country", "weather");
        assertThat(events).extracting(ChatEvent::type)
                .containsExactly("answer_delta", "tool_finished", "tool_finished", "done");
        assertThat(events.getFirst().text()).isEqualTo(capital + ": 16.8 C");
        assertThat(events.get(1).name()).isEqualTo("get_country");
        assertThat(events.get(2).name()).isEqualTo("get_weather");
        assertThat(events.get(2).input()).contains(capital);
        verify(model, times(3)).stream(any(Prompt.class));
    }

    private static AssistantMessage toolCall(String id, String name, String arguments) {
        return AssistantMessage.builder().content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, arguments))).build();
    }

    record WeatherRequest(String city) {
    }

    record CapitalResult(String capital) {
    }

    record TemperatureResult(String temperature) {
    }

    @Test
    void streamingChatExposesProviderToolsAndReturnsToolResultToModelWithEmptyRagContext() {
        // given
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

        // when
        var events = new ChatService(client).sendMessage("What is the capital of Poland?")
                .collectList().block(Duration.ofSeconds(10));
        String answer = events.stream().filter(event -> event.type().equals("answer_delta"))
                .map(ChatEvent::text).collect(Collectors.joining());

        // then
        assertThat(events).extracting(ChatEvent::type)
                .containsExactly("answer_delta", "tool_finished", "done");
        var finished = events.stream().filter(event -> event.type().equals("tool_finished")).findFirst().orElseThrow();
        assertThat(finished.input()).contains("Poland");
        assertThat(finished.text()).contains("Warsaw", "12345678");
        assertThat(finished.name()).isEqualTo("get_country");

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
