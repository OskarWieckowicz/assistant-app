package com.assistant_app.assistant_app.rag;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.rag.ingestion.enabled=false")
class CdqRagChatIT {

        @Autowired
        private ChatClient chatClient;

        @Test
        void shouldAnswerUsingCdqKnowledge() {
                // when
                String answer = chatClient.prompt()
                                .user("How does CDQ Fraud Guard assess whether a bank account can be trusted?")
                                .call()
                                .content();

                // then
                assertThat(answer).isNotBlank();
                assertThat(answer).containsIgnoringCase("Trust Score");
        }

        @Test
        void shouldAnswerGeneralQuestionWhenNoCdqDocumentsMatch() {
                // when
                var response = chatClient.prompt()
                                .user("what is capital city of Germany?")
                                .call()
                                .chatResponse();

                // then
                assertThat(response).isNotNull();
                List<Document> documents = response.getMetadata()
                                .get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
                assertThat(documents).as("The question should exercise empty retrieval").isEmpty();
                assertThat(response.getResult().getOutput().getText())
                                .containsIgnoringCase("Berlin")
                                .doesNotContainIgnoringCase("context", "cannot answer", "can't answer");
        }

        @Test
        void shouldStreamGeneralAnswerWhenNoCdqDocumentsMatch() {
                // when
                String answer = chatClient.prompt()
                                .user("what is capital city of Germany?")
                                .stream()
                                .content()
                                .collectList()
                                .map(chunks -> String.join("", chunks))
                                .block(java.time.Duration.ofMinutes(2));

                // then
                assertThat(answer)
                                .containsIgnoringCase("Berlin")
                                .doesNotContainIgnoringCase("context", "cannot answer", "can't answer");
        }
}
