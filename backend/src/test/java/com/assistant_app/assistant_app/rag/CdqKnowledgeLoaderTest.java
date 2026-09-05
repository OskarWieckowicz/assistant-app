package com.assistant_app.assistant_app.rag;

import org.junit.jupiter.api.Test;
import java.util.List;
import org.springframework.ai.document.Document;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;
import java.util.Objects;
import java.util.stream.Collectors;

public class CdqKnowledgeLoaderTest {
        Resource resource = new ClassPathResource("knowledge/cdq-fraud-guard.txt");

        CdqKnowledgeLoader loader = new CdqKnowledgeLoader(resource);

        @Test
        void load_returnsCdqDocumentWithContentAndMetadata() {
                List<Document> documents = loader.load();
                assertThat(documents).hasSizeGreaterThan(1);

                Document document = documents.get(0);
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

                String combinedContent = documents.stream()
                                .map(chunk -> Objects.requireNonNullElse(chunk.getText(), ""))
                                .collect(Collectors.joining("\n"));

                assertThat(combinedContent)
                                .contains("CDQ Fraud Guard")
                                .contains("Trust Score")
                                .contains("Bank account verification");
        }
}
