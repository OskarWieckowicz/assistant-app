package com.assistant_app.assistant_app.rag;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CdqKnowledgeIngestionService {
    private final CdqKnowledgeLoader knowledgeLoader;
    private final VectorStore vectorStore;

    private static final String CDQ_KNOWLEDGE_FILTER = "knowledge_base == 'cdq-fraud-guard'";

    public int ingest() {
        List<Document> documents = knowledgeLoader.load();
        if (documents.isEmpty()) {
            throw new IllegalStateException("No CDQ knowledge documents to ingest");
        }
        vectorStore.delete(CDQ_KNOWLEDGE_FILTER);
        vectorStore.add(documents);

        return documents.size();
    }

}
