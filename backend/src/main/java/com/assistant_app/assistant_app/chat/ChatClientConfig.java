package com.assistant_app.assistant_app.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;

@Configuration
public class ChatClientConfig {

    @Bean
    ChatClient chatClient(
            ChatClient.Builder builder,
            VectorStore vectorStore) {
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(2)
                .similarityThresholdAll()
                .filterExpression("knowledge_base == 'cdq-fraud-guard'")
                .build();

        QuestionAnswerAdvisor ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

        return builder
                .defaultSystem("""
                        You are a helpful AI assistant.

                        For questions about CDQ products, use the supplied knowledge context.
                        If the context is insufficient, clearly say that the provided CDQ
                        material does not contain the answer.

                        Do not invent CDQ product capabilities.
                        """)
                .defaultAdvisors(ragAdvisor)
                .build();
    }
}
