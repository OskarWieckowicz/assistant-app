package com.assistant_app.assistant_app.rag;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Requires PostgreSQL, Ollama, and previously ingested CDQ knowledge.
@SpringBootTest(properties = {
                "app.rag.ingestion.enabled=false",
                "spring.ai.ollama.chat.options.temperature=0"
})
class CdqRagEvaluationIT {

        @Autowired
        private ChatClient chatClient;

        @Autowired
        private ChatModel chatModel;

        @ParameterizedTest(name = "[{index}] {0}")
        @ValueSource(strings = {
                        "How is the CDQ Fraud Guard Trust Score determined?",
                        "How does CDQ Fraud Guard verify new bank accounts?",
                        "How can CDQ Fraud Guard integrate with existing systems?",
        })
        void shouldAnswerConsistentlyWithRetrievedContext(String question) {
                var response = chatClient.prompt()
                                .user(question)
                                .call()
                                .chatResponse();

                assertThat(response).as("Chat response for: %s", question).isNotNull();

                String answer = response.getResult().getOutput().getText();
                List<Document> documents = response.getMetadata()
                                .get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);

                assertThat(answer).as("Answer for: %s", question).isNotBlank();
                assertThat(documents).as("Retrieved context for: %s", question).isNotEmpty();

                var evaluator = new RelevancyEvaluator(ChatClient.builder(chatModel));
                var evaluation = evaluator.evaluate(new EvaluationRequest(question, documents, answer));

                assertThat(evaluation.isPass())
                                .withFailMessage("Question: %s%nAnswer: %s%nContext: %s", question, answer, documents)
                                .isTrue();
        }
}
