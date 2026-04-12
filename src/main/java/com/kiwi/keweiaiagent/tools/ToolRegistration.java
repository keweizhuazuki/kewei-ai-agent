package com.kiwi.keweiaiagent.tools;

import com.kiwi.keweiaiagent.agent.ManusSessionStore;
import com.kiwi.keweiaiagent.agent.todo.CommunityTodoMapper;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具注册配置类，负责集中装配并导出全部可用工具。
 */
@Configuration
public class ToolRegistration {
    /**
     * 导出长期记忆专用工具集合，便于单独注入到聊天链路。
     */
    @Bean(name = "memoryTools")
    public ToolCallback[] memoryTools(
            MemoryViewTool memoryViewTool,
            MemoryCreateTool memoryCreateTool,
            MemoryStrReplaceTool memoryStrReplaceTool,
            MemoryInsertTool memoryInsertTool,
            MemoryDeleteTool memoryDeleteTool,
            MemoryRenameTool memoryRenameTool
    ) {
        return ToolCallbacks.from(
                memoryViewTool,
                memoryCreateTool,
                memoryStrReplaceTool,
                memoryInsertTool,
                memoryDeleteTool,
                memoryRenameTool
        );
    }

    /**
     * 注册 TodoWrite 社区工具，并在写入时同步待办快照。
     */
    @Bean
    public TodoWriteTool todoWriteTool(ManusSessionStore manusSessionStore) {
        return TodoWriteTool.builder()
                .todoEventHandler(todos -> {
                    String sessionId = manusSessionStore.currentSessionId();
                    if (sessionId != null && !sessionId.isBlank()) {
                        manusSessionStore.saveTodoSnapshot(sessionId, CommunityTodoMapper.toSnapshot(todos));
                    }
                })
                .build();
    }

    /**
     * 汇总并导出系统内全部可用工具回调。
     */
    @Bean
    public ToolCallback[] allTools(
            EmailTool emailTool,
            FileOperationTool fileOperationTool,
            PdfConvertTool pdfConvertTool,
            ResourceDownloadTool resourceDownloadTool,
            TimeTool timeTool,
            MemoryViewTool memoryViewTool,
            MemoryCreateTool memoryCreateTool,
            MemoryStrReplaceTool memoryStrReplaceTool,
            MemoryInsertTool memoryInsertTool,
            MemoryDeleteTool memoryDeleteTool,
            MemoryRenameTool memoryRenameTool,
            OpenClawResearchTool openClawResearchTool,
            TerminateTool terminateTool,
            AskUserQuestionTool askUserQuestionTool,
            PptWriterTool pptWriterTool,
            TodoWriteToolAdapter todoWriteToolAdapter
    ){
        return ToolCallbacks.from(
                emailTool,
                fileOperationTool,
                pdfConvertTool,
                resourceDownloadTool,
                timeTool,
                memoryViewTool,
                memoryCreateTool,
                memoryStrReplaceTool,
                memoryInsertTool,
                memoryDeleteTool,
                memoryRenameTool,
                openClawResearchTool,
                terminateTool,
                askUserQuestionTool,
                pptWriterTool,
                todoWriteToolAdapter
        );
    }
}
