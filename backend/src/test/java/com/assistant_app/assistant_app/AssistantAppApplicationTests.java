package com.assistant_app.assistant_app;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.convention.TestBean;

@SpringBootTest(
		classes = { AssistantAppApplication.class, PostgresTestcontainersConfiguration.class },
		properties = "spring.ai.mcp.client.enabled=false")
class AssistantAppApplicationTests {

	@TestBean
	ToolCallbackProvider toolCallbackProvider;

	static ToolCallbackProvider toolCallbackProvider() {
		return ToolCallbackProvider.from();
	}

	@Test
	void contextLoads() {
	}

}
