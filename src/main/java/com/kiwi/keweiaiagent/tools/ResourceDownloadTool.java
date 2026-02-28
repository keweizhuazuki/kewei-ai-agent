package com.kiwi.keweiaiagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.kiwi.keweiaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ResourceDownloadTool {

    private final String DOWNLOAD_DIR = FileConstant.File_SAVE_DIR + "/download";

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
            FileUtil.mkdir(DOWNLOAD_DIR);
            String targetPath = DOWNLOAD_DIR + "/" + fileName;
            HttpUtil.downloadFile(url, targetPath);
            return "Resource downloaded successfully to: " + targetPath;
        } catch (Exception e) {
            return "Error downloading resource: " + e.getMessage();
        }
    }
}
