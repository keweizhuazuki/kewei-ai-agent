package com.kiwi.keweiaiagent.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongTermMemoryPromptServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldInjectConfiguredMemoryRootIntoPromptTemplate() {
        Path configuredRoot = tempDir.resolve("test-memory-root");
        LongTermMemoryPromptService service = new LongTermMemoryPromptService(
                new ByteArrayResource("memory root: {{MEMORIES_ROOT_DIRECTORY}}".getBytes(StandardCharsets.UTF_8)),
                configuredRoot
        );

        String prompt = service.buildPrompt();

        assertEquals("memory root: " + configuredRoot.toString(), prompt);
    }

    @Test
    void shouldClosePromptStreamAfterSuccessfulRead() {
        CloseTrackingInputStream stream = new CloseTrackingInputStream(new ByteArrayInputStream(
                "记忆根目录: {{MEMORIES_ROOT_DIRECTORY}}".getBytes(StandardCharsets.UTF_8)));
        LongTermMemoryPromptService service = new LongTermMemoryPromptService(
                new InputStreamResource(stream), tempDir);

        assertEquals("记忆根目录: " + tempDir.toString(), service.buildPrompt());
        assertTrue(stream.closed, "Prompt input stream should be closed after reading");
    }

    @Test
    void shouldClosePromptStreamAndPreserveReadFailure() {
        IOException readFailure = new IOException("Synthetic read failure");
        CloseTrackingInputStream stream = new CloseTrackingInputStream(new InputStream() {
            @Override
            public int read() throws IOException {
                throw readFailure;
            }
        });
        LongTermMemoryPromptService service = new LongTermMemoryPromptService(
                new InputStreamResource(stream), tempDir);

        UncheckedIOException failure = assertThrows(UncheckedIOException.class, service::buildPrompt);

        assertEquals("Unable to load long-term memory prompt", failure.getMessage());
        assertSame(readFailure, failure.getCause());
        assertTrue(stream.closed, "Prompt input stream should be closed after a read failure");
    }

    private static final class CloseTrackingInputStream extends FilterInputStream {
        private boolean closed;

        private CloseTrackingInputStream(InputStream input) {
            super(input);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
