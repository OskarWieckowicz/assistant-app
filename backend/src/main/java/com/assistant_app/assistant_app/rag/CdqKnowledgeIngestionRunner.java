package com.assistant_app.assistant_app.rag;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rag.ingestion.enabled", havingValue = "true")
@Slf4j
class CdqKnowledgeIngestionRunner implements ApplicationRunner {

    private final CdqKnowledgeIngestionService ingestionService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int size = ingestionService.ingest();
        log.info("Ingested {} CDQ knowledge documents", size);
    }
}