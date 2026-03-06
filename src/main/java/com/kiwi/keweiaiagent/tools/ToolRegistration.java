package com.kiwi.keweiaiagent.tools;

import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中管理工具类的注册和实例化逻辑，提供一个统一的入口来获取各种工具实例。
 */
@Configuration
public class ToolRegistration {
    @Bean
    public ToolCallback[] allTools(
            EmailTool emailTool,
            FileOperationTool fileOperationTool,
            PdfConvertTool pdfConvertTool,
            ResourceDownloadTool resourceDownloadTool,
            TimeTool timeTool,
            WebSearchTool webSearchTool,
            WebScrapingTool webScrapingTool,
            TerminateTool terminateTool,
            AskUserQuestionTool askUserQuestionTool,
            PptWriterTool pptWriterTool
    ){
        return ToolCallbacks.from(
                emailTool,
                fileOperationTool,
                pdfConvertTool,
                resourceDownloadTool,
                timeTool,
                webSearchTool,
                webScrapingTool,
                terminateTool,
                askUserQuestionTool,
                pptWriterTool
        );
    }
}
