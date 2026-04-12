package com.kiwi.keweiaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * 重命名或移动长期记忆文件。
 */
@Component
public class MemoryRenameTool {

    private final MemoryToolSupport support;

    public MemoryRenameTool(@Qualifier("longTermMemoriesRootPath") Path memoriesRootPath) {
        this.support = new MemoryToolSupport(memoriesRootPath);
    }

    @Tool(name = "MemoryRename", description = "Rename or move a memory file within the sandboxed memories directory", returnDirect = false)
    public String memoryRename(
            @ToolParam(description = "Current relative path of the memory file or directory") String sourcePath,
            @ToolParam(description = "New relative path for the memory file or directory") String targetPath
    ) {
        try {
            Path source = support.resolve(sourcePath);
            Path target = support.resolve(targetPath);
            support.move(source, target);
            return "Renamed memory path: " + support.display(source) + " -> " + support.display(target);
        } catch (Exception e) {
            return "Error renaming memory: " + e.getMessage();
        }
    }
}
