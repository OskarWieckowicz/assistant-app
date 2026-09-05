package com.assistant_app.assistant_app.rag;

import org.springframework.stereotype.Component;

import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;

@Component
public class CdqKnowledgeLoader {
    private final Resource resource;

    CdqKnowledgeLoader(@Value("classpath:knowledge/cdq-fraud-guard.txt") Resource resource) {
        this.resource = resource;
    }

    public List<Document> load() {
        TextReader reader = new TextReader(resource);

        reader.getCustomMetadata()
                .put("knowledge_base", "cdq-fraud-guard");

        reader.getCustomMetadata()
                .put(
                        "source_url",
                        "https://www.cdq.com/products/cdq-fraud-guard");

        reader.getCustomMetadata()
                .put("title", "CDQ Fraud Guard");

        List<Document> documents = reader.get();
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(250)
                .withMinChunkSizeChars(100)
                .withKeepSeparator(true)
                .build();

        return splitter.apply(documents);
    }

}
