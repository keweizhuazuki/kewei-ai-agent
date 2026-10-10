package com.kiwi.keweiaiagent.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.kiwi.keweiaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ResourceDownloadTool {

    private final FileToolPathSupport paths;

    public ResourceDownloadTool() {
        this(Path.of(FileConstant.File_SAVE_DIR, "download"));
    }

    ResourceDownloadTool(Path directory) {
        this.paths = new FileToolPathSupport(directory);
    }

    @Tool(description = "Download a resource from URL to local download directory",returnDirect = false)
    public String downloadResource(
            @ToolParam(description = "URL of the resource to download") String url,
            @ToolParam(description = "Target file name to save as") String fileName
    ) {
        if (StrUtil.isBlank(url)) {
            return "Error: url is required.";
        }
        if (StrUtil.isBlank(fileName)) {
            return "Error: fileName is required.";
        }

        try {
            Path targetPath = paths.resolveFile(fileName);
            Files.createDirectories(targetPath.getParent());
            HttpUtil.downloadFile(url, targetPath.toFile());
            return "Resource downloaded successfully to: " + targetPath;
        } catch (Exception e) {
            return "Error downloading resource: " + e.getMessage();
        }
    }
}
