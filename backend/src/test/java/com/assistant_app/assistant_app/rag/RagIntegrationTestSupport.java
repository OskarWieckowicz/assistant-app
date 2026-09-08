package com.assistant_app.assistant_app.rag;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.convention.TestBean;

import com.assistant_app.assistant_app.AssistantAppApplication;
import com.assistant_app.assistant_app.PostgresTestcontainersConfiguration;

@SpringBootTest(
        classes = { AssistantAppApplication.class, PostgresTestcontainersConfiguration.class },
        properties = {
                "app.rag.ingestion.enabled=true",
                "spring.ai.mcp.client.enabled=false",
                "spring.ai.ollama.chat.options.temperature=0"
        })
abstract class RagIntegrationTestSupport {

    @TestBean
    ToolCallbackProvider toolCallbackProvider;

    static ToolCallbackProvider toolCallbackProvider() {
        return ToolCallbackProvider.from();
    }
}
