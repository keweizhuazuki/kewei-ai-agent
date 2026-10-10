package com.kiwi.keweiaiagent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
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

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectEmptyReplacementTargetWithoutHangingOrChangingFile(String oldText) throws Exception {
        Path file = tempDir.resolve("profile.md");
        Files.writeString(file, "The user prefers concise answers.\n");
        String original = Files.readString(file);

        String result = assertTimeoutPreemptively(Duration.ofSeconds(1),
                () -> memoryStrReplaceTool.memoryStrReplace("profile.md", oldText, "replacement"));

        assertEquals("Error replacing memory text: target text must not be null or empty", result);
        assertEquals(original, Files.readString(file));
    }

    @Test
    void shouldRejectEmptyReplacementTargetInEmptyFile() throws Exception {
        Path file = tempDir.resolve("empty.md");
        Files.writeString(file, "");

        String result = assertTimeoutPreemptively(Duration.ofSeconds(1),
                () -> memoryStrReplaceTool.memoryStrReplace("empty.md", "", "replacement"));

        assertEquals("Error replacing memory text: target text must not be null or empty", result);
        assertEquals("", Files.readString(file));
    }

    @Test
    void shouldAllowUniqueWhitespaceReplacementAndEmptyReplacementText() throws Exception {
        Path file = tempDir.resolve("profile.md");
        Files.writeString(file, "first second");

        String result = memoryStrReplaceTool.memoryStrReplace("profile.md", " ", "");

        assertTrue(result.contains("Replaced text"));
        assertEquals("firstsecond", Files.readString(file));
    }

    @Test
    void shouldRejectMissingAndRepeatedReplacementTargetsWithoutChangingFile() throws Exception {
        Path file = tempDir.resolve("profile.md");
        Files.writeString(file, "same same");

        assertTrue(memoryStrReplaceTool.memoryStrReplace("profile.md", "missing", "new")
                .contains("target text not found"));
        assertTrue(memoryStrReplaceTool.memoryStrReplace("profile.md", "same", "new")
                .contains("target text is not unique"));
        assertEquals("same same", Files.readString(file));
    }
}
