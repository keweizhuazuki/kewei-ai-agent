package com.kiwi.keweiaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 在长期记忆文件指定行后插入新内容。
 */
@Component
public class MemoryInsertTool {

    private final MemoryToolSupport support;

    public MemoryInsertTool(@Qualifier("longTermMemoriesRootPath") Path memoriesRootPath) {
        this.support = new MemoryToolSupport(memoriesRootPath);
    }

    @Tool(name = "MemoryInsert", description = "Insert text after a given line number in an existing memory file", returnDirect = false)
    public String memoryInsert(
            @ToolParam(description = "Relative path of the memory file to update") String relativePath,
            @ToolParam(description = "Insert after this 1-based line number. Use 0 to insert at the beginning.") Integer afterLine,
            @ToolParam(description = "Text to insert") String textToInsert
    ) {
        try {
            Path path = support.resolve(relativePath);
            List<String> lines = new ArrayList<>(support.readLines(path));
            int lineNumber = afterLine == null ? lines.size() : afterLine;
            if (lineNumber < 0 || lineNumber > lines.size()) {
                return "Error inserting memory text: line number out of range for " + support.display(path);
            }
            List<String> insertedLines = Arrays.asList((textToInsert == null ? "" : textToInsert).split("\\R", -1));
            lines.addAll(lineNumber, insertedLines);
            support.writeLines(path, lines);
            return "Inserted text into memory file: " + support.display(path);
        } catch (Exception e) {
            return "Error inserting memory text: " + e.getMessage();
        }
    }
}
