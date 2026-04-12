package com.kiwi.keweiaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 对现有长期记忆文件执行精确字符串替换。
 */
@Component
public class MemoryStrReplaceTool {

    private final MemoryToolSupport support;

    public MemoryStrReplaceTool(@Qualifier("longTermMemoriesRootPath") Path memoriesRootPath) {
        this.support = new MemoryToolSupport(memoriesRootPath);
    }

    @Tool(name = "MemoryStrReplace", description = "Replace an exact and unique string inside an existing memory file", returnDirect = false)
    public String memoryStrReplace(
            @ToolParam(description = "Relative path of the memory file to update") String relativePath,
            @ToolParam(description = "Exact string to replace. It must appear exactly once.") String oldText,
            @ToolParam(description = "Replacement text") String newText
    ) {
        try {
            Path path = support.resolve(relativePath);
            String content = Files.readString(path);
            int occurrences = support.countOccurrences(content, oldText);
            if (occurrences == 0) {
                return "Error replacing memory text: target text not found in " + support.display(path);
            }
            if (occurrences > 1) {
                return "Error replacing memory text: target text is not unique in " + support.display(path);
            }
            support.writeFile(path, content.replace(oldText, newText));
            return "Replaced text in memory file: " + support.display(path);
        } catch (Exception e) {
            return "Error replacing memory text: " + e.getMessage();
        }
    }
}
