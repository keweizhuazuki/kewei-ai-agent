package com.kiwi.keweiaiagent.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 集中管理工具类的注册和实例化逻辑，提供一个统一的入口来获取各种工具实例。
 */
@Configuration
public class ToolRegistration {
    @Bean
    public ToolCallback[] allTools(){
        return ToolCallbacks.from(
                new FileOperationTool(),
                new PdfConvertTool(),
                new ResourceDownloadTool(),
                new WebSearchTool(),
                new WebScrapingTool()
        );
    }
}
