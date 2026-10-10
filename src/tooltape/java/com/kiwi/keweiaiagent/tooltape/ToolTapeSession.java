package com.kiwi.keweiaiagent.tooltape;

import dev.tooltape.CapturePolicy;
import dev.tooltape.Json;
import dev.tooltape.Recorder;
import dev.tooltape.Replay;
import dev.tooltape.Tape;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One serial tool-boundary session, shared by the callbacks in a single fixture.
 * This records tools, not model responses, prompts, or whole agent executions.
 * ToolContext is passed through in memory and is never submitted to ToolTape.
 */
public final class ToolTapeSession implements AutoCloseable {
    /** Allowlisting this field captures the complete returned string, including any secrets in it. */
    public static final String RESULT_FIELD = "result";
    private final Recorder recorder;
    private final Replay replay;
    private final ThreadLocal<Invocation> invocation = new ThreadLocal<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private boolean closed;

    private ToolTapeSession(Path path, CapturePolicy policy, boolean record) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(policy);
        if (record) {
            recorder = new Recorder(path, (name, arguments) -> {
                Invocation current = Objects.requireNonNull(invocation.get());
                String result = current.withContext()
                        ? current.delegate().call(current.input(), current.context())
                        : current.delegate().call(current.input());
                return Collections.singletonMap(RESULT_FIELD, result);
            }, policy, "kewei-ai-agent.spring-ai-tools");
            replay = null;
        } else {
            recorder = null;
            replay = new Replay(Tape.read(path), policy);
        }
    }

    public static ToolTapeSession record(Path path) throws IOException {
        return record(path, CapturePolicy.safe());
    }

    public static ToolTapeSession record(Path path, CapturePolicy policy) throws IOException {
        return new ToolTapeSession(path, policy, true);
    }

    public static ToolTapeSession replay(Path path) throws IOException {
        return replay(path, CapturePolicy.safe());
    }

    public static ToolTapeSession replay(Path path, CapturePolicy policy) throws IOException {
        return new ToolTapeSession(path, policy, false);
    }

    /** Replay retains only the definition and metadata, never the executable live callback. */
    public ToolCallback wrap(ToolCallback callback) {
        Objects.requireNonNull(callback);
        return new TapedCallback(callback.getToolDefinition(), callback.getToolMetadata(),
                recorder == null ? null : callback);
    }

    private String call(String name, ToolCallback delegate, String input, ToolContext context,
                        boolean withContext) {
        enter();
        try {
            if (closed) {
                throw new IllegalStateException("ToolTape session is closed");
            }
            // Parse only for matching/capture. The recording delegate gets the original text.
            Map<String, Object> arguments = Json.object(input);
            if (recorder != null) {
                invocation.set(new Invocation(delegate, input, context, withContext));
                return (String) recorder.call(name, arguments).get(RESULT_FIELD);
            }
            Map<String, Object> result = replay.call(name, arguments);
            // A placeholder could change orchestration while masquerading as faithful replay.
            if (!result.containsKey(RESULT_FIELD)) {
                throw new IllegalStateException("ToolTape response was not captured; replay requires "
                        + "a reviewed fixture recorded with kewei.tooltape.capture-output=true");
            }
            Object value = result.get(RESULT_FIELD);
            if (value != null && !(value instanceof String)) {
                throw new IllegalStateException("ToolTape result must be a string or null");
            }
            return (String) value;
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("ToolTape tool call failed", failure);
        } finally {
            invocation.remove();
            busy.set(false);
        }
    }

    private void enter() {
        if (!busy.compareAndSet(false, true)) {
            throw new IllegalStateException("ToolTape requires serial, non-recursive tool calls");
        }
    }

    @Override
    public void close() throws IOException {
        enter();
        try {
            if (closed) {
                return;
            }
            closed = true;
            if (recorder != null) {
                recorder.close();
            } else {
                replay.close(); // Fails if the fixture still has unconsumed calls.
            }
        } finally {
            busy.set(false);
        }
    }

    private record Invocation(ToolCallback delegate, String input, ToolContext context,
                              boolean withContext) { }

    private final class TapedCallback implements ToolCallback {
        private final ToolDefinition definition;
        private final ToolMetadata metadata;
        private final ToolCallback recordingDelegate;

        private TapedCallback(ToolDefinition definition, ToolMetadata metadata,
                              ToolCallback recordingDelegate) {
            this.definition = definition;
            this.metadata = metadata;
            this.recordingDelegate = recordingDelegate;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return definition;
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return metadata;
        }

        @Override
        public String call(String toolInput) {
            return ToolTapeSession.this.call(definition.name(), recordingDelegate,
                    toolInput, null, false);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            return ToolTapeSession.this.call(definition.name(), recordingDelegate,
                    toolInput, toolContext, true);
        }
    }
}
