package com.assistant_app.assistant_app.chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(ChatController.class)
class ChatControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChatService chatService;

    @ParameterizedTest
    @ValueSource(strings = { "{}", "{\"message\":null}", "{\"message\":\"\"}", "{\"message\":\"   \\t\\n\"}" })
    void rejectsMissingOrBlankMessageBeforeStartingStream(String body) throws Exception {
        // given
        var request = post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM).content(body);

        // when
        var result = mvc.perform(request);

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(request().asyncNotStarted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("message must not be blank"));
        verifyNoInteractions(chatService);
    }

    @Test
    void streamsValidMessage() throws Exception {
        // given
        given(chatService.sendMessage("Hello")).willReturn(Flux.just(
                ChatEvent.text("answer_delta", "Hi"), ChatEvent.text("done", null)));

        // when
        var result = mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM).content("{\"message\":\"Hello\"}"))
                .andExpect(request().asyncStarted()).andReturn();
        // then
        mvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("\"type\":\"answer_delta\"")))
                .andExpect(content().string(containsString("\"type\":\"done\"")));
        verify(chatService).sendMessage("Hello");
    }
}
