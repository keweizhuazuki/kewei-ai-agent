package com.kiwi.keweiaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * 删除长期记忆文件或目录。
 */
@Component
public class MemoryDeleteTool {

    private final MemoryToolSupport support;

    public MemoryDeleteTool(@Qualifier("longTermMemoriesRootPath") Path memoriesRootPath) {
        this.support = new MemoryToolSupport(memoriesRootPath);
    }

    @Tool(name = "MemoryDelete", description = "Delete a memory file or directory inside the sandboxed memories directory", returnDirect = false)
    public String memoryDelete(
            @ToolParam(description = "Relative path of the memory file or directory to delete") String relativePath
    ) {
        try {
            Path path = support.resolve(relativePath);
            support.deleteRecursively(path);
            return "Deleted memory path: " + support.display(path);
        } catch (Exception e) {
            return "Error deleting memory: " + e.getMessage();
        }
    }
}
