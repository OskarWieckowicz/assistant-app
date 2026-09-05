package com.assistant_app.assistant_app.rag;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CdqKnowledgeIngestionServiceTest {

    @Mock
    private CdqKnowledgeLoader knowledgeLoader;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private CdqKnowledgeIngestionService ingestionService;

    @Test
    void ingest_shouldIngestDocuments() {
        // given
        List<Document> documents = List.of(new Document("test"));
        when(knowledgeLoader.load()).thenReturn(documents);

        // when
        int size = ingestionService.ingest();

        // then
        assertThat(size).isEqualTo(documents.size());
        verify(vectorStore).delete("knowledge_base == 'cdq-fraud-guard'");
        verify(vectorStore).add(documents);
    }

    @Test
    void ingest_shouldThrowWhenNoDocuments() {
        // given
        when(knowledgeLoader.load()).thenReturn(List.of());

        // when / then
        assertThatThrownBy(() -> ingestionService.ingest())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No CDQ knowledge documents to ingest");

        verify(vectorStore, never()).delete(anyString());
        verify(vectorStore, never()).add(any());
    }
}
