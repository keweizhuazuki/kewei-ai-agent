package com.kiwi.keweiaiagent.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MemoryPathSafetyTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRejectFileLinksForReadReplaceInsertDeleteAndRename() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("memory"));
        Path outside = Files.writeString(tempDir.resolve("outside.md"), "original");
        Path link = Files.createSymbolicLink(root.resolve("link.md"), outside);

        assertRejected(new MemoryViewTool(root).memoryView("link.md"));
        assertRejected(new MemoryStrReplaceTool(root).memoryStrReplace("link.md", "original", "changed"));
        assertRejected(new MemoryInsertTool(root).memoryInsert("link.md", 0, "changed"));
        assertRejected(new MemoryDeleteTool(root).memoryDelete("link.md"));
        assertRejected(new MemoryRenameTool(root).memoryRename("link.md", "moved.md"));

        assertEquals("original", Files.readString(outside));
        assertTrue(Files.isSymbolicLink(link));
        assertFalse(Files.exists(root.resolve("moved.md")));
    }

    @Test
    void shouldRejectDirectoryLinksIncludingMissingDescendants() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("memory"));
        Path outside = Files.createDirectory(tempDir.resolve("outside"));
        Files.writeString(outside.resolve("existing.md"), "original");
        Files.createSymbolicLink(root.resolve("linked"), outside);
        Files.writeString(root.resolve("source.md"), "source");

        assertRejected(new MemoryViewTool(root).memoryView("linked"));
        assertRejected(new MemoryCreateTool(root).memoryCreate("linked/nested/new.md", "changed"));
        assertRejected(new MemoryDeleteTool(root).memoryDelete("linked/existing.md"));
        assertRejected(new MemoryRenameTool(root).memoryRename("source.md", "linked/moved.md"));

        assertEquals("original", Files.readString(outside.resolve("existing.md")));
        assertEquals("source", Files.readString(root.resolve("source.md")));
        assertFalse(Files.exists(outside.resolve("nested")));
        assertFalse(Files.exists(outside.resolve("moved.md")));
    }

    @Test
    void shouldRejectDanglingLinksInsteadOfCreatingTheirTargets() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("memory"));
        Path outside = tempDir.resolve("missing.md");
        Files.createSymbolicLink(root.resolve("link.md"), outside);

        assertRejected(new MemoryCreateTool(root).memoryCreate("link.md", "changed"));

        assertFalse(Files.exists(outside));
    }

    @Test
    void shouldAllowConfiguredRootAliasAndOrdinaryNestedFiles() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("memory"));
        Path alias = Files.createSymbolicLink(tempDir.resolve("alias"), root);

        String result = new MemoryCreateTool(alias).memoryCreate("nested/profile.md", "content");

        assertTrue(result.startsWith("Created memory file:"));
        assertEquals("content", Files.readString(root.resolve("nested/profile.md")));
        assertTrue(new MemoryViewTool(alias).memoryView("nested/profile.md").contains("1: content"));
    }

    @Test
    void shouldRejectRootReplacedByLinkAfterToolInitialization() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("memory"));
        Path outside = Files.createDirectory(tempDir.resolve("outside"));
        MemoryCreateTool tool = new MemoryCreateTool(root);
        Files.delete(root);
        Files.createSymbolicLink(root, outside);

        assertRejected(tool.memoryCreate("profile.md", "changed"));

        assertFalse(Files.exists(outside.resolve("profile.md")));
    }

    private void assertRejected(String result) {
        assertTrue(result.contains("Symbolic links are not allowed"), result);
    }
}
