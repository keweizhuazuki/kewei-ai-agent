package com.kiwi.keweiaiagent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryToolsTest {

    @TempDir
    Path tempDir;

    private MemoryViewTool memoryViewTool;
    private MemoryCreateTool memoryCreateTool;
    private MemoryStrReplaceTool memoryStrReplaceTool;
    private MemoryInsertTool memoryInsertTool;
    private MemoryDeleteTool memoryDeleteTool;
    private MemoryRenameTool memoryRenameTool;

    @BeforeEach
    void setUp() {
        memoryViewTool = new MemoryViewTool(tempDir);
        memoryCreateTool = new MemoryCreateTool(tempDir);
        memoryStrReplaceTool = new MemoryStrReplaceTool(tempDir);
        memoryInsertTool = new MemoryInsertTool(tempDir);
        memoryDeleteTool = new MemoryDeleteTool(tempDir);
        memoryRenameTool = new MemoryRenameTool(tempDir);
    }

    @Test
    void shouldCreateAndViewMemoryFileWithLineNumbers() throws Exception {
        String content = """
                ---
                name: user profile
                description: prefers concise answers
                type: user
                ---

                The user prefers concise answers.
                """;

        String createResult = memoryCreateTool.memoryCreate("user_profile.md", content);

        assertTrue(createResult.contains("Created memory file"));
        assertTrue(Files.exists(tempDir.resolve("user_profile.md")));

        String viewResult = memoryViewTool.memoryView("user_profile.md");

        assertTrue(viewResult.contains("1: ---"));
        assertTrue(viewResult.contains("2: name: user profile"));
        assertTrue(viewResult.contains("7: The user prefers concise answers."));
    }

    @Test
    void shouldListDirectoryInsertReplaceRenameAndDeleteMemoryFiles() throws Exception {
        memoryCreateTool.memoryCreate("MEMORY.md", "- [User Profile](user_profile.md) - concise answers");
        memoryCreateTool.memoryCreate("user_profile.md", "The user prefers concise answers.");

        String listResult = memoryViewTool.memoryView(".");
        assertTrue(listResult.contains("MEMORY.md"));
        assertTrue(listResult.contains("user_profile.md"));

        String insertResult = memoryInsertTool.memoryInsert("MEMORY.md", 1,
                "- [Project Notes](project_notes.md) - migrate to CockroachDB");
        assertTrue(insertResult.contains("Inserted text"));
        assertTrue(Files.readString(tempDir.resolve("MEMORY.md")).contains("Project Notes"));

        memoryCreateTool.memoryCreate("project_notes.md", "Migrate to PostgreSQL.");
        String replaceResult = memoryStrReplaceTool.memoryStrReplace(
                "project_notes.md",
                "PostgreSQL",
                "CockroachDB"
        );
        assertTrue(replaceResult.contains("Replaced text"));
        assertTrue(Files.readString(tempDir.resolve("project_notes.md")).contains("CockroachDB"));

        String renameResult = memoryRenameTool.memoryRename("project_notes.md", "project_history.md");
        assertTrue(renameResult.contains("Renamed memory path"));
        assertTrue(Files.exists(tempDir.resolve("project_history.md")));
        assertFalse(Files.exists(tempDir.resolve("project_notes.md")));

        String deleteResult = memoryDeleteTool.memoryDelete("project_history.md");
        assertTrue(deleteResult.contains("Deleted memory path"));
        assertFalse(Files.exists(tempDir.resolve("project_history.md")));
    }

    @Test
    void shouldRejectPathsOutsideMemoryRoot() {
        String createResult = memoryCreateTool.memoryCreate("../escape.md", "nope");
        String viewResult = memoryViewTool.memoryView("../escape.md");
        String renameResult = memoryRenameTool.memoryRename("MEMORY.md", "../escape.md");
        String deleteResult = memoryDeleteTool.memoryDelete("../escape.md");

        assertTrue(createResult.contains("outside memory root"));
        assertTrue(viewResult.contains("outside memory root"));
        assertTrue(renameResult.contains("outside memory root"));
        assertTrue(deleteResult.contains("outside memory root"));
    }
}
