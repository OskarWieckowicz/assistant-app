package com.assistant_app.assistant_app.chat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

	@Mock
	private ChatModel chatModel;

	private ChatService chatService;

	@BeforeEach
	void setUp() {
		given(chatModel.getOptions()).willReturn(ToolCallingChatOptions.builder().build());
		chatService = new ChatService(ChatClient.create(chatModel));
	}

	@Test
	void sendMessage_streamsModelContentChunks() {
		// given
		String message = "Hi";
		given(chatModel.stream(any(Prompt.class)))
			.willReturn(Flux.just(chatResponse("Hello"), chatResponse(" "), chatResponse("world")));

		// when
		Flux<ChatEvent> response = chatService.sendMessage(message);

		// then
		StepVerifier.create(response)
			.expectNext(ChatEvent.text("answer_delta", "Hello"), ChatEvent.text("answer_delta", " "),
                ChatEvent.text("answer_delta", "world"), ChatEvent.text("done", null))
			.verifyComplete();
	}

	@Test
	void sendMessage_forwardsUserMessageToModel() {
		// given
		String message = "What's the weather?";
		given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(chatResponse("ok")));

		// when
		Flux<ChatEvent> response = chatService.sendMessage(message);

		// then
		StepVerifier.create(response)
			.expectNext(ChatEvent.text("answer_delta", "ok"), ChatEvent.text("done", null))
			.verifyComplete();

		ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
		verify(chatModel).stream(promptCaptor.capture());
		assertThat(promptCaptor.getValue().getUserMessage().getText()).isEqualTo(message);
	}

	@Test
	void sendMessage_completesWhenModelEmitsNothing() {
		// given
		String message = "Hi";
		given(chatModel.stream(any(Prompt.class))).willReturn(Flux.empty());

		// when
		Flux<ChatEvent> response = chatService.sendMessage(message);

		// then
		StepVerifier.create(response).expectNext(ChatEvent.text("done", null)).verifyComplete();
	}

	@Test
	void sendMessage_emitsErrorEvent() {
		// given
		String message = "Hi";
		given(chatModel.stream(any(Prompt.class)))
			.willReturn(Flux.error(new IllegalStateException("model unavailable")));

		// when
		Flux<ChatEvent> response = chatService.sendMessage(message);

		// then
		StepVerifier.create(response)
            .expectNext(ChatEvent.text("error", "Could not complete the response."))
            .verifyComplete();
	}

    @Test
    void streamsThinkingSeparatelyAndSkipsEmptyChunks() {
        // given
        var thinking = AssistantMessage.builder().content("")
                .properties(java.util.Map.of("thinking", "Checking…")).build();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(
                new ChatResponse(List.of(new Generation(thinking))), chatResponse(""), chatResponse("Warsaw")));

        // when / then
        StepVerifier.create(chatService.sendMessage("What is the capital of Poland?"))
                .expectNext(ChatEvent.text("thinking_delta", "Checking…"),
                        ChatEvent.text("answer_delta", "Warsaw"), ChatEvent.text("done", null))
                .verifyComplete();
    }

    @Test
    void cancellingResponseCancelsModelSubscription() throws InterruptedException {
        // given
        var cancelled = new java.util.concurrent.CountDownLatch(1);
        given(chatModel.stream(any(Prompt.class))).willReturn(
                Flux.concat(Flux.just(chatResponse("Hello")), Flux.<ChatResponse>never())
                        .doOnCancel(cancelled::countDown));

        // when
        StepVerifier.create(chatService.sendMessage("Hi"))
                .expectNext(ChatEvent.text("answer_delta", "Hello"))
                .thenCancel().verify(java.time.Duration.ofSeconds(5));

        // then
        assertThat(cancelled.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsCollectedToolsBeforeModelError() {
        // given
        var tool = ChatEvent.tool("tool_failed", "1", "country", "{}", "Tool failed.");
        given(chatModel.stream(any(Prompt.class))).willAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            var options = (org.springframework.ai.model.tool.ToolCallingChatOptions) prompt.getOptions();
            var history = (List<ChatEvent>) options.getToolContext().get(ActivityToolCallback.TOOL_CALLS);
            return Flux.defer(() -> {
                history.add(tool);
                return Flux.concat(Flux.just(chatResponse("Partial answer")),
                        Flux.error(new IllegalStateException("Model failed")));
            });
        });

        // when / then
        StepVerifier.create(chatService.sendMessage("Hi"))
                .expectNext(ChatEvent.text("answer_delta", "Partial answer"), tool,
                        ChatEvent.text("error", "Could not complete the response."))
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void eachSubscriptionGetsItsOwnToolHistory() {
        // given
        var tool = ChatEvent.tool("tool_finished", "1", "country", "{}", "Warsaw");
        given(chatModel.stream(any(Prompt.class))).willAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            var options = (org.springframework.ai.model.tool.ToolCallingChatOptions) prompt.getOptions();
            var history = (List<ChatEvent>) options.getToolContext().get(ActivityToolCallback.TOOL_CALLS);
            return Flux.defer(() -> {
                assertThat(history).isEmpty();
                history.add(tool);
                return Flux.just(chatResponse("Warsaw"));
            });
        });

        // when
        var response = chatService.sendMessage("Hi");

        // then
        for (int i = 0; i < 2; i++) {
            StepVerifier.create(response)
                    .expectNext(ChatEvent.text("answer_delta", "Warsaw"), tool, ChatEvent.text("done", null))
                    .verifyComplete();
        }
    }

	private static ChatResponse chatResponse(String content) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
	}

}
