package com.kiwi.keweiaiagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiwi.keweiaiagent.agent.ManusSessionStore;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRegistrationTest {

    @Test
    void shouldUseRemoteResearchToolInDefaultToolSet() {
        ToolRegistration registration = new ToolRegistration();

        ToolCallback[] tools = registration.allTools(
                new EmailTool(),
                new FileOperationTool(),
                new PdfConvertTool(),
                new ResourceDownloadTool(),
                new TimeTool(),
                new OpenClawResearchTool(
                        new ObjectMapper(),
                        (command, timeout) -> new OpenClawCommandRunner.CommandResult(0, "{\"summary\":\"ok\"}", ""),
                        "openclaw",
                        "main",
                        120,
                        "spring-ai-research"
                ),
                new TerminateTool(),
                AskUserQuestionTool.builder()
                        .questionHandler(questions -> java.util.Map.of())
                        .build(),
                new PptWriterTool(),
                new TodoWriteToolAdapter(TodoWriteTool.builder()
                        .todoEventHandler(todos -> { })
                        .build())
        );

        Set<String> toolNames = Arrays.stream(tools)
                .map(tool -> tool.getToolDefinition().name())
                .collect(Collectors.toSet());

        assertTrue(toolNames.contains("delegateResearchToOpenClaw"));
        assertFalse(toolNames.contains("searchWebsite"));
        assertFalse(toolNames.contains("scrapeWebsite"));
    }
}
