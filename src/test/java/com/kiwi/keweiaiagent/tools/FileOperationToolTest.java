package com.kiwi.keweiaiagent.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileOperationToolTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldReadAndWriteNestedUtf8Files() throws Exception {
        FileOperationTool tool = new FileOperationTool(tempDir);
        assertTrue(tool.writeFile("nested/profile.txt", "中文内容").startsWith("File written successfully"));
        assertEquals("中文内容", tool.readFile("nested/profile.txt"));
        assertEquals("中文内容", Files.readString(tempDir.resolve("nested/profile.txt")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "..", "../outside.txt", "."})
    void shouldRejectInvalidFileNames(String name) {
        FileOperationTool tool = new FileOperationTool(tempDir);
        assertTrue(tool.readFile(name).startsWith("Error reading file:"));
        assertTrue(tool.writeFile(name, "changed").startsWith("Error writing file:"));
    }

    @Test
    void shouldRejectAbsolutePathsAndKeepOutsideFilesUnchanged() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("files"));
        Path outside = Files.writeString(tempDir.resolve("outside.txt"), "original");
        FileOperationTool tool = new FileOperationTool(root);
        for (String name : new String[] {outside.toString(), "../outside.txt"}) {
            assertTrue(tool.readFile(name).contains("outside tool directory"));
            assertTrue(tool.writeFile(name, "changed").contains("outside tool directory"));
        }
        assertEquals("original", Files.readString(outside));
    }

    @Test
    void shouldRejectSymbolicLinksAndDirectoryTargets() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("files"));
        Path outside = Files.createDirectory(tempDir.resolve("outside"));
        Files.writeString(outside.resolve("profile.txt"), "original");
        Files.createSymbolicLink(root.resolve("linked"), outside);
        Files.createSymbolicLink(root.resolve("dangling"), tempDir.resolve("missing.txt"));
        Files.createDirectory(root.resolve("nested"));
        FileOperationTool tool = new FileOperationTool(root);

        assertTrue(tool.readFile("linked/profile.txt").contains("Symbolic links"));
        assertTrue(tool.writeFile("linked/new.txt", "changed").contains("Symbolic links"));
        assertTrue(tool.writeFile("dangling", "changed").contains("Symbolic links"));
        assertTrue(tool.writeFile("nested", "changed").contains("is a directory"));
        assertEquals("original", Files.readString(outside.resolve("profile.txt")));
        assertFalse(Files.exists(outside.resolve("new.txt")));
        assertFalse(Files.exists(tempDir.resolve("missing.txt")));
    }
}
