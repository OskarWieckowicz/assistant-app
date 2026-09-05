package com.assistant_app.assistant_app.rag;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.client.ChatClient;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.rag.ingestion.enabled=false")
class CdqRagChatIT {

    @Autowired
    private ChatClient chatClient;

    @Test
    void shouldAnswerUsingCdqKnowledge() {
        String answer = chatClient.prompt()
                .user("How does CDQ Fraud Guard assess whether a bank account can be trusted?")
                .call()
                .content();

        assertThat(answer).isNotBlank();
        assertThat(answer).containsIgnoringCase("Trust Score");
    }
}
