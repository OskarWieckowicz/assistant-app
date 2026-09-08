package com.assistant_app.assistant_app.rag;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class CdqVectorStoreIT extends RagIntegrationTestSupport {

    @Autowired
    private VectorStore vectorStore;

    @Test
    void similaritySearch_findsTrustScoreInformation() {
        // given
        SearchRequest request = SearchRequest.builder()
                .query("How does CDQ assess whether a bank account can be trusted?")
                .topK(2)
                .filterExpression(
                        "knowledge_base == 'cdq-fraud-guard'")
                .build();

        // when
        List<Document> results = vectorStore.similaritySearch(request);

        // then
        assertThat(results)
                .isNotEmpty()
                .hasSizeLessThanOrEqualTo(2);

        assertThat(results).allSatisfy(document -> {
            assertThat(document.getText()).isNotBlank();

            assertThat(document.getMetadata())
                    .containsEntry(
                            "knowledge_base",
                            "cdq-fraud-guard");
        });

        assertThat(results).anySatisfy(document -> assertThat(document.getText())
                .containsIgnoringCase("Trust Score"));
    }
}
