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
import org.springframework.ai.chat.prompt.ChatOptions;
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
		given(chatModel.getOptions()).willReturn(ChatOptions.builder().build());
		chatService = new ChatService(ChatClient.create(chatModel));
	}

	@Test
	void sendMessage_streamsModelContentChunks() {
		// given
		String message = "Hi";
		given(chatModel.stream(any(Prompt.class)))
			.willReturn(Flux.just(chatResponse("Hello"), chatResponse(" "), chatResponse("world")));

		// when
		Flux<String> response = chatService.sendMessage(message);

		// then
		StepVerifier.create(response)
			.expectNext("Hello", " ", "world")
			.verifyComplete();
	}

	@Test
	void sendMessage_forwardsUserMessageToModel() {
		// given
		String message = "What's the weather?";
		given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(chatResponse("ok")));

		// when
		Flux<String> response = chatService.sendMessage(message);

		// then
		StepVerifier.create(response)
			.expectNext("ok")
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
		Flux<String> response = chatService.sendMessage(message);

		// then
		StepVerifier.create(response).verifyComplete();
	}

	@Test
	void sendMessage_propagatesModelError() {
		// given
		String message = "Hi";
		given(chatModel.stream(any(Prompt.class)))
			.willReturn(Flux.error(new IllegalStateException("model unavailable")));

		// when
		Flux<String> response = chatService.sendMessage(message);

		// then
		StepVerifier.create(response)
			.expectErrorSatisfies(error -> assertThat(error)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("model unavailable"))
			.verify();
	}

	private static ChatResponse chatResponse(String content) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
	}

}
