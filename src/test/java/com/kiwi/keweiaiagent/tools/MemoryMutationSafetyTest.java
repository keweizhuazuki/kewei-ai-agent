package com.kiwi.keweiaiagent.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class MemoryMutationSafetyTest {

    @TempDir
    Path root;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", ".", "nested/.."})
    void shouldRejectDeletingRootWithoutLosingMemory(String path) throws Exception {
        Files.writeString(root.resolve("MEMORY.md"), "important");

        String result = new MemoryDeleteTool(root).memoryDelete(path);

        assertTrue(result.contains("Cannot delete or rename the memory root"), result);
        assertEquals("important", Files.readString(root.resolve("MEMORY.md")));
    }

    @Test
    void shouldRejectRenamingRootOrReplacingIt() throws Exception {
        Files.writeString(root.resolve("MEMORY.md"), "important");
        Files.createDirectory(root.resolve("nested"));
        MemoryRenameTool tool = new MemoryRenameTool(root);

        assertTrue(tool.memoryRename(".", "moved").contains("Cannot delete or rename the memory root"));
        assertTrue(tool.memoryRename("nested", ".").contains("Cannot delete or rename the memory root"));
        assertEquals("important", Files.readString(root.resolve("MEMORY.md")));
        assertTrue(Files.isDirectory(root.resolve("nested")));
    }

    @Test
    void shouldNotOverwriteAnExistingRenameTarget() throws Exception {
        Files.writeString(root.resolve("source.md"), "source");
        Files.writeString(root.resolve("target.md"), "target");

        String result = new MemoryRenameTool(root).memoryRename("source.md", "target.md");

        assertTrue(result.contains("Memory target already exists"), result);
        assertEquals("source", Files.readString(root.resolve("source.md")));
        assertEquals("target", Files.readString(root.resolve("target.md")));
    }

    @Test
    void shouldAtomicallyCreateWithoutOverwritingConcurrentContent() throws Exception {
        MemoryToolSupport support = new MemoryToolSupport(root);
        Path target = root.resolve("profile.md");
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> createAfterSignal(support, target, "first", start));
            var second = executor.submit(() -> createAfterSignal(support, target, "second", start));
            start.countDown();
            String firstResult = first.get();
            String secondResult = second.get();
            assertTrue((firstResult == null) != (secondResult == null), "Only one creation should succeed");
            assertEquals(firstResult == null ? secondResult : firstResult, Files.readString(target));
        }
    }

    @Test
    void shouldStillDeleteAndMoveNestedDirectories() throws Exception {
        Files.createDirectories(root.resolve("nested"));
        Files.writeString(root.resolve("nested/profile.md"), "content");

        assertTrue(new MemoryRenameTool(root).memoryRename("nested", "archive/nested")
                .startsWith("Renamed memory path:"));
        assertEquals("content", Files.readString(root.resolve("archive/nested/profile.md")));
        assertTrue(new MemoryDeleteTool(root).memoryDelete("archive").startsWith("Deleted memory path:"));
        assertTrue(Files.isDirectory(root));
        assertFalse(Files.exists(root.resolve("archive")));
    }

    private String createAfterSignal(MemoryToolSupport support, Path target, String content,
                                     CountDownLatch start) throws Exception {
        start.await();
        try {
            support.createFile(target, content);
            return content;
        } catch (FileAlreadyExistsException expected) {
            return null;
        }
    }
}
