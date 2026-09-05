package com.assistant_app.assistant_app.rag;

import org.junit.jupiter.api.Test;
import java.util.List;
import org.springframework.ai.document.Document;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;

public class CdqKnowledgeLoaderTest {
    Resource resource = new ClassPathResource("knowledge/cdq-fraud-guard.txt");

    CdqKnowledgeLoader loader = new CdqKnowledgeLoader(resource);

    @Test
    void load_returnsCdqDocumentWithContentAndMetadata() {
        List<Document> documents = loader.load();
        assertThat(documents).hasSize(1);
        Document document = documents.getFirst();
        assertThat(document.getText())
                .contains("CDQ Fraud Guard")
                .contains("Trust Score")
                .contains("Bank account verification");
        assertThat(document.getMetadata())
                .containsEntry("knowledge_base", "cdq-fraud-guard")
                .containsEntry("title", "CDQ Fraud Guard")
                .containsEntry(
                        "source_url",
                        "https://www.cdq.com/products/cdq-fraud-guard")
                .containsEntry("source", "cdq-fraud-guard.txt")
                .containsEntry("charset", "UTF-8");
    }
}
