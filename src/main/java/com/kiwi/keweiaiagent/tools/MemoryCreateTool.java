package com.kiwi.keweiaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 创建新的长期记忆文件。
 */
@Component
public class MemoryCreateTool {

    private final MemoryToolSupport support;

    public MemoryCreateTool(@Qualifier("longTermMemoriesRootPath") Path memoriesRootPath) {
        this.support = new MemoryToolSupport(memoriesRootPath);
    }

    @Tool(name = "MemoryCreate", description = "Create a new memory file inside the sandboxed memories directory", returnDirect = false)
    public String memoryCreate(
            @ToolParam(description = "Relative path of the memory file to create") String relativePath,
            @ToolParam(description = "Content to write into the new memory file") String content
    ) {
        try {
            Path path = support.resolve(relativePath);
            if (Files.exists(path)) {
                return "Error creating memory: memory path already exists: " + support.display(path);
            }
            support.writeFile(path, content);
            return "Created memory file: " + support.display(path);
        } catch (Exception e) {
            return "Error creating memory: " + e.getMessage();
        }
    }
}
