package com.kiwi.keweiaiagent.tooltape;

import dev.tooltape.CapturePolicy;
import dev.tooltape.Json;
import dev.tooltape.Replay;
import dev.tooltape.Tape;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ToolTapeSessionTest {
    @TempDir Path directory;
    private static final CapturePolicy FIXTURE_POLICY =
            CapturePolicy.allow(Set.of("city"), Set.of(ToolTapeSession.RESULT_FIELD));

    @Test
    void recordingPreservesDefinitionMetadataOriginalJsonAndContext() throws Exception {
        Path tapePath = directory.resolve("context.tape");
        ToolContext context = new ToolContext(Map.of("session", "private-context-value"));
        String originalJson = "{ \"city\" : \"Paris\", \"apiKey\" : \"private-input-value\" }";
        var calls = new AtomicInteger();
        ToolDefinition definition = definition("forecast");
        ToolMetadata metadata = ToolMetadata.builder().returnDirect(true).build();
        ToolCallback delegate = new ToolCallback() {
            public ToolDefinition getToolDefinition() { return definition; }
            public ToolMetadata getToolMetadata() { return metadata; }
            public String call(String input) { fail("Wrong call overload"); return null; }
            public String call(String input, ToolContext actualContext) {
                calls.incrementAndGet();
                assertSame(originalJson, input);
                assertSame(context, actualContext);
                return "private-result-value";
            }
        };
        try (ToolTapeSession recording = ToolTapeSession.record(tapePath)) {
            ToolCallback wrapped = recording.wrap(delegate);
            assertSame(definition, wrapped.getToolDefinition());
            assertSame(metadata, wrapped.getToolMetadata());
            assertEquals("private-result-value", wrapped.call(originalJson, context));
        }
        assertEquals(1, calls.get());
        Tape tape = Tape.read(tapePath);
        assertEquals(2, tape.version());
        assertTrue(tape.complete());
        assertEquals("{}", tape.events().getFirst().input());
        assertEquals("{}", tape.events().getFirst().result());
        // Inspect decoded fields, not only the base64-encoded file bytes.
        assertFalse(tape.toString().contains("private-"));
        assertEquals("kewei-ai-agent.spring-ai-tools", tape.metadata().source());
    }

    @Test
    void safeReplayFailsClearlyWithoutInvokingEitherLiveOverload() throws Exception {
        Path path = directory.resolve("safe.tape");
        try (ToolTapeSession recording = ToolTapeSession.record(path)) {
            recording.wrap(callback("forecast", new AtomicInteger(), "private-response"))
                    .call("{\"city\":\"Paris\"}");
        }
        var calls = new AtomicInteger();
        try (ToolTapeSession replay = ToolTapeSession.replay(path)) {
            // Uncaptured argument values cannot be compared; only order/name are checked.
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> replay.wrap(callback("forecast", calls, "LIVE"))
                            .call("{\"city\":\"London\"}", new ToolContext(Map.of())));
            assertTrue(failure.getMessage().contains("response was not captured"));
        }
        assertEquals(0, calls.get());
    }

    @Test
    void explicitlyAllowedFixtureReplaysExactTextAndCanonicalArgumentsOffline() throws Exception {
        Path path = directory.resolve("fixture.tape");
        String expected = "Sunny ☀\nSynthetic fixture";
        try (ToolTapeSession recording = ToolTapeSession.record(path, FIXTURE_POLICY)) {
            assertEquals(expected, recording.wrap(callback("forecast", new AtomicInteger(), expected))
                    .call("{\"city\":\"Paris\",\"token\":\"omitted\"}"));
        }
        var calls = new AtomicInteger();
        try (ToolTapeSession replay = ToolTapeSession.replay(path, FIXTURE_POLICY)) {
            assertEquals(expected, replay.wrap(callback("forecast", calls, "unexpected"))
                    .call("{ \"city\" : \"Paris\" }"));
        }
        assertEquals(0, calls.get());
        assertEquals(Map.of("city", "Paris"), Json.object(Tape.read(path).events().getFirst().input()));
    }

    @Test
    void credentialNamesAndNestedValuesStayRedactedEvenIfAllowlisted() throws Exception {
        Path path = directory.resolve("allowlist.tape");
        CapturePolicy policy = CapturePolicy.allow(Set.of("city", "apiKey", "nested"), Set.of());
        try (ToolTapeSession recording = ToolTapeSession.record(path, policy)) {
            recording.wrap(callback("forecast", new AtomicInteger(), "private-output"))
                    .call("{\"city\":\"Paris\",\"apiKey\":\"private-key\",\"nested\":{\"value\":\"private-nested\"}}");
        }
        Tape tape = Tape.read(path);
        assertEquals(Map.of("city", "Paris", "nested", CapturePolicy.REDACTED),
                Json.object(tape.events().getFirst().input()));
        assertFalse(tape.toString().contains("private-"));
    }

    @Test
    void credentialNamesWithLineBreaksAndInvisibleCharactersStayOmittedEvenIfAllowlisted() throws Exception {
        Path path = directory.resolve("disguised-credentials.tape");
        CapturePolicy policy = CapturePolicy.allow(
                Set.of("city", "password\n", "api\nkey", "api\u200b_key"), Set.of());
        String input = Json.write(Map.of(
                "city", "Paris",
                "password\n", "private-password",
                "api\nkey", "private-line-break-key",
                "api\u200b_key", "private-invisible-key"));
        var calls = new AtomicInteger();
        try (ToolTapeSession recording = ToolTapeSession.record(path, policy)) {
            recording.wrap(callback("forecast", calls, "private-output")).call(input);
        }
        assertEquals(1, calls.get());
        Tape tape = Tape.read(path);
        assertEquals(Map.of("city", "Paris"), Json.object(tape.events().getFirst().input()));
        assertEquals(Map.of(), Json.object(tape.events().getFirst().result()));
        assertFalse(tape.toString().contains("private-"));
    }

    @Test
    void inputThatFitsJsonButExceedsEncodedTapeBudgetFailsBeforeInvokingDelegate() throws Exception {
        // ASCII needs no JSON escaping, but base64 expansion pushes this past the tape limit.
        String input = "{\"city\":\"" + "x".repeat(13 * 1024 * 1024) + "\"}";
        assertTrue(input.length() < 16 * 1024 * 1024);
        assertTrue(4L * ((input.length() + 2) / 3) > Tape.MAX_BYTES);
        assertDoesNotThrow(() -> Json.object(input), "The fixture must fit the JSON limit");
        var calls = new AtomicInteger();
        CapturePolicy policy = CapturePolicy.allow(Set.of("city"), Set.of());
        try (ToolTapeSession recording = ToolTapeSession.record(directory.resolve("oversized.tape"), policy)) {
            ToolCallback wrapped = recording.wrap(callback("forecast", calls, "live"));
            IllegalStateException failure = assertThrows(IllegalStateException.class, () -> wrapped.call(input));
            IOException cause = assertInstanceOf(IOException.class, failure.getCause());
            assertAll(
                    () -> assertEquals(0, calls.get(),
                            "Encoded tape capacity must be checked before executing the delegate"),
                    () -> assertTrue(cause.getMessage().contains("tool was not executed")));
        }
    }

    @Test
    void mismatchNeverFallsBackToLiveCallback() throws Exception {
        Path path = directory.resolve("mismatch.tape");
        writeFixture(path);
        var calls = new AtomicInteger();
        ToolTapeSession replay = ToolTapeSession.replay(path, FIXTURE_POLICY);
        assertThrows(Replay.Divergence.class,
                () -> replay.wrap(callback("forecast", calls, "live")).call("{\"city\":\"London\"}"));
        assertThrows(Replay.Divergence.class, replay::close);
        assertEquals(0, calls.get());
    }

    @Test
    void incompleteReplayFailsWhenClosed() throws Exception {
        Path path = directory.resolve("unconsumed.tape");
        writeFixture(path);
        ToolTapeSession replay = ToolTapeSession.replay(path, FIXTURE_POLICY);
        assertThrows(Replay.Divergence.class, replay::close);
    }

    @Test
    void missingTapeWrongPolicyAndUnfinishedRecordingFailBeforeAnyCall() throws Exception {
        assertThrows(IOException.class, () -> ToolTapeSession.replay(directory.resolve("missing.tape")));
        Path complete = directory.resolve("policy.tape");
        writeFixture(complete);
        assertThrows(IllegalArgumentException.class, () -> ToolTapeSession.replay(complete));
        Path incomplete = directory.resolve("unfinished.tape");
        try (ToolTapeSession recording = ToolTapeSession.record(incomplete)) {
            assertThrows(IllegalArgumentException.class, () -> ToolTapeSession.replay(incomplete));
        }
    }

    @Test
    void recordingNeverOverwritesAndInvalidJsonCannotExecuteLiveTool() throws Exception {
        Path path = directory.resolve("existing.tape");
        Files.writeString(path, "keep me");
        assertThrows(IOException.class, () -> ToolTapeSession.record(path));
        assertEquals("keep me", Files.readString(path));
        var calls = new AtomicInteger();
        try (ToolTapeSession recording = ToolTapeSession.record(directory.resolve("invalid.tape"))) {
            assertThrows(IllegalArgumentException.class,
                    () -> recording.wrap(callback("forecast", calls, "live")).call("not JSON"));
        }
        assertEquals(0, calls.get());
    }

    @Test
    void errorsKeepOriginalRecordingExceptionButNeverPersistItsMessage() throws Exception {
        Path path = directory.resolve("errors.tape");
        IllegalArgumentException original = new IllegalArgumentException("private-error-detail");
        ToolCallback failing = new ToolCallback() {
            public ToolDefinition getToolDefinition() { return definition("failure"); }
            public String call(String input) { throw original; }
        };
        try (ToolTapeSession recording = ToolTapeSession.record(path)) {
            assertSame(original, assertThrows(IllegalArgumentException.class,
                    () -> recording.wrap(failing).call("{}")));
        }
        Tape tape = Tape.read(path);
        assertEquals("java.lang.IllegalArgumentException", tape.events().getFirst().result());
        assertFalse(tape.toString().contains("private-error-detail"));
        var calls = new AtomicInteger();
        try (ToolTapeSession replay = ToolTapeSession.replay(path)) {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> replay.wrap(callback("failure", calls, "live")).call("{}"));
            assertInstanceOf(Replay.RecordedToolException.class, error.getCause());
        }
        assertEquals(0, calls.get());
    }

    @Test
    void concurrentCallsAreRejectedRatherThanInterleaved() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        ToolCallback blocking = new ToolCallback() {
            public ToolDefinition getToolDefinition() { return definition("blocking"); }
            public String call(String input) {
                entered.countDown();
                try {
                    assertTrue(release.await(10, TimeUnit.SECONDS));
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(failure);
                }
                return "ok";
            }
        };
        try (ToolTapeSession recording = ToolTapeSession.record(directory.resolve("serial.tape"));
             var executor = Executors.newSingleThreadExecutor()) {
            ToolCallback wrapped = recording.wrap(blocking);
            var first = executor.submit(() -> wrapped.call("{}"));
            try {
                assertTrue(entered.await(10, TimeUnit.SECONDS));
                assertThrows(IllegalStateException.class, () -> wrapped.call("{}"));
                assertThrows(IllegalStateException.class, recording::close);
            } finally {
                release.countDown();
            }
            assertEquals("ok", first.get(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void closedSessionCannotRunLiveTool() throws Exception {
        var calls = new AtomicInteger();
        ToolTapeSession recording = ToolTapeSession.record(directory.resolve("closed.tape"));
        ToolCallback callback = recording.wrap(callback("forecast", calls, "live"));
        recording.close();
        recording.close();
        assertThrows(IllegalStateException.class, () -> callback.call("{}"));
        assertEquals(0, calls.get());
    }

    private void writeFixture(Path path) throws Exception {
        try (ToolTapeSession recording = ToolTapeSession.record(path, FIXTURE_POLICY)) {
            recording.wrap(callback("forecast", new AtomicInteger(), "sunny"))
                    .call("{\"city\":\"Paris\"}");
        }
    }

    static ToolDefinition definition(String name) {
        return ToolDefinition.builder().name(name).description("Synthetic offline fixture")
                .inputSchema("{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}}}")
                .build();
    }

    static ToolCallback callback(String name, AtomicInteger calls, String result) {
        return new ToolCallback() {
            public ToolDefinition getToolDefinition() { return definition(name); }
            public String call(String input) { calls.incrementAndGet(); return result; }
            public String call(String input, ToolContext context) { calls.incrementAndGet(); return result; }
        };
    }
}
