package com.kiwi.keweiaiagent.tooltape;

import dev.tooltape.Tape;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.kiwi.keweiaiagent.tooltape.ToolTapeSessionTest.callback;
import static org.junit.jupiter.api.Assertions.*;

class ToolTapeBeanPostProcessorTest {
    @TempDir Path directory;

    @Test
    void profileWithDefaultModeLeavesOriginalCallbacksAndFilesystemUntouched() throws Exception {
        ToolCallback[] original = { callback("forecast", new AtomicInteger(), "live") };
        try (var context = context(Map.of(), original, original)) {
            assertSame(original, context.getBean("allTools"));
            assertSame(original, context.getBean("memoryTools"));
        }
        try (var paths = Files.list(directory)) {
            assertEquals(0, paths.count());
        }
    }

    @Test
    void springRegistrationWrapsBothArraysSharesOrderAndClosesRecorder() throws Exception {
        Path path = directory.resolve("spring.tape");
        var recordingCalls = new AtomicInteger();
        ToolCallback[] allTools = { callback("forecast", recordingCalls, "sunny") };
        ToolCallback[] memoryTools = { callback("memory", recordingCalls, "saved") };
        Map<String, Object> recordProperties = Map.of("kewei.tooltape.mode", "record",
                "kewei.tooltape.path", path.toString());
        try (var context = context(recordProperties, allTools, memoryTools)) {
            ToolCallback[] tapedAll = context.getBean("allTools", ToolCallback[].class);
            ToolCallback[] tapedMemory = context.getBean("memoryTools", ToolCallback[].class);
            assertNotSame(allTools, tapedAll);
            assertNotSame(memoryTools, tapedMemory);
            assertEquals("sunny", tapedAll[0].call("{}"));
            assertEquals("saved", tapedMemory[0].call("{}"));
        }
        assertEquals(2, recordingCalls.get());
        Tape tape = Tape.read(path);
        assertTrue(tape.complete());
        assertEquals(2, tape.events().size());
        assertEquals("forecast", tape.events().get(0).tool());
        assertEquals("memory", tape.events().get(1).tool());
        var replayCalls = new AtomicInteger();
        Map<String, Object> replayProperties = Map.of("kewei.tooltape.mode", "replay",
                "kewei.tooltape.path", path.toString());
        try (var context = context(replayProperties,
                new ToolCallback[] { callback("forecast", replayCalls, "live") },
                new ToolCallback[] { callback("memory", replayCalls, "live") })) {
            assertThrows(IllegalStateException.class,
                    () -> context.getBean("allTools", ToolCallback[].class)[0].call("{}"));
            assertThrows(IllegalStateException.class,
                    () -> context.getBean("memoryTools", ToolCallback[].class)[0].call("{}"));
        }
        assertEquals(0, replayCalls.get());
    }

    @Test
    void captureFieldsAndOutputAreExplicitRuntimeOptIns() throws Exception {
        Path path = directory.resolve("fixture.tape");
        Map<String, Object> properties = Map.of("kewei.tooltape.mode", "record",
                "kewei.tooltape.path", path.toString(), "kewei.tooltape.input-fields", " city, ,city ",
                "kewei.tooltape.capture-output", "true");
        ToolCallback[] callbacks = { callback("forecast", new AtomicInteger(), "sunny") };
        try (var context = context(properties, callbacks, new ToolCallback[0])) {
            context.getBean("allTools", ToolCallback[].class)[0]
                    .call("{\"city\":\"Paris\",\"other\":\"omitted\"}");
        }
        Tape tape = Tape.read(path);
        assertEquals("{\"city\":\"Paris\"}", tape.events().getFirst().input());
        assertEquals("{\"result\":\"sunny\"}", tape.events().getFirst().result());
    }

    @Test
    void unrelatedBeansAreNeverWrapped() throws Exception {
        ToolTapeBeanPostProcessor processor = new ToolTapeBeanPostProcessor(new MockEnvironment()
                .withProperty("kewei.tooltape.mode", "record")
                .withProperty("kewei.tooltape.path", directory.resolve("untouched.tape").toString()));
        try {
            ToolCallback[] tools = { callback("forecast", new AtomicInteger(), "live") };
            assertSame(tools, processor.postProcessAfterInitialization(tools, "otherTools"));
            Object ordinaryBean = new Object();
            assertSame(ordinaryBean, processor.postProcessAfterInitialization(ordinaryBean, "ordinary"));
        } finally {
            processor.destroy();
        }
    }

    @Test
    void invalidModeAndMissingPathFailFast() {
        assertThrows(IllegalArgumentException.class, () -> new ToolTapeBeanPostProcessor(
                new MockEnvironment().withProperty("kewei.tooltape.mode", "typo")));
        assertThrows(IllegalArgumentException.class, () -> new ToolTapeBeanPostProcessor(
                new MockEnvironment().withProperty("kewei.tooltape.mode", "record")));
        assertThrows(IllegalArgumentException.class, () -> new ToolTapeBeanPostProcessor(
                new MockEnvironment().withProperty("kewei.tooltape.mode", "replay")
                        .withProperty("kewei.tooltape.path", "  ")));
    }

    private AnnotationConfigApplicationContext context(Map<String, Object> properties,
                                                       ToolCallback[] allTools,
                                                       ToolCallback[] memoryTools) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("fixture", properties));
        context.register(ToolTapeConfiguration.class);
        context.registerBean("allTools", ToolCallback[].class, () -> allTools);
        context.registerBean("memoryTools", ToolCallback[].class, () -> memoryTools);
        context.refresh();
        return context;
    }
}
