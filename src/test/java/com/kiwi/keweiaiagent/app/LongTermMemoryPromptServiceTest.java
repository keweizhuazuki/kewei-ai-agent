package com.kiwi.keweiaiagent.app;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LongTermMemoryPromptServiceTest {

    @Test
    void shouldInjectConfiguredMemoryRootIntoPromptTemplate() {
        LongTermMemoryPromptService service = new LongTermMemoryPromptService(
                new ByteArrayResource("memory root: {{MEMORIES_ROOT_DIRECTORY}}".getBytes()),
                Path.of("/tmp/test-memory-root")
        );

        String prompt = service.buildPrompt();

        assertTrue(prompt.contains("/tmp/test-memory-root"));
    }
}
