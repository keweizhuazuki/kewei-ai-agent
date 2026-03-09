package com.kiwi.keweiaiagent.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManusSessionServiceTest {

    @Test
    void shouldSelectPptToolSubsetForPptPrompt() {
        ManusSessionService service = new ManusSessionService();
        ToolCallback ask = tool("AskUserQuestionTool");
        ToolCallback todo = tool("TodoWrite");
        ToolCallback research = tool("delegateResearchToOpenClaw");
        ToolCallback ppt = tool("create_pptx");
        ToolCallback terminate = tool("doTerminate");
        ToolCallback email = tool("sendEmail");
        ReflectionTestUtils.setField(service, "allTools", new ToolCallback[]{ask, todo, research, ppt, terminate, email});

        ToolCallback[] selected = service.selectToolsForPrompt("帮我做个ppt，内容是静安寺约会地点推荐。3页");

        assertEquals(5, selected.length);
        assertTrue(contains(selected, "AskUserQuestionTool"));
        assertTrue(contains(selected, "TodoWrite"));
        assertTrue(contains(selected, "delegateResearchToOpenClaw"));
        assertTrue(contains(selected, "create_pptx"));
        assertTrue(contains(selected, "doTerminate"));
    }

    @Test
    void shouldSelectEmailToolSubsetForEmailPrompt() {
        ManusSessionService service = new ManusSessionService();
        ToolCallback ask = tool("AskUserQuestionTool");
        ToolCallback todo = tool("TodoWrite");
        ToolCallback email = tool("sendEmail");
        ToolCallback file = tool("writeFile");
        ToolCallback search = tool("searchWebsite");
        ToolCallback terminate = tool("doTerminate");
        ToolCallback[] all = new ToolCallback[]{ask, todo, email, file, search, terminate};
        ReflectionTestUtils.setField(service, "allTools", all);

        ToolCallback[] selected = service.selectToolsForPrompt("帮我发一封邮件给客户");

        assertEquals(5, selected.length);
        assertTrue(contains(selected, "AskUserQuestionTool"));
        assertTrue(contains(selected, "TodoWrite"));
        assertTrue(contains(selected, "sendEmail"));
        assertTrue(contains(selected, "writeFile"));
        assertTrue(contains(selected, "doTerminate"));
    }

    @Test
    void shouldSelectPdfToolSubsetForPdfPrompt() {
        ManusSessionService service = new ManusSessionService();
        ToolCallback ask = tool("AskUserQuestionTool");
        ToolCallback todo = tool("TodoWrite");
        ToolCallback download = tool("downloadResource");
        ToolCallback read = tool("readFile");
        ToolCallback write = tool("writeFile");
        ToolCallback pdf = tool("pdfToImages");
        ToolCallback terminate = tool("doTerminate");
        ToolCallback search = tool("searchWebsite");
        ReflectionTestUtils.setField(service, "allTools", new ToolCallback[]{ask, todo, download, read, write, pdf, terminate, search});

        ToolCallback[] selected = service.selectToolsForPrompt("帮我把这个PDF转成图片");

        assertEquals(7, selected.length);
        assertTrue(contains(selected, "TodoWrite"));
        assertTrue(contains(selected, "downloadResource"));
        assertTrue(contains(selected, "pdfToImages"));
        assertTrue(contains(selected, "doTerminate"));
    }

    @Test
    void shouldKeepAllToolsForGeneralPrompt() {
        ManusSessionService service = new ManusSessionService();
        ToolCallback email = tool("sendEmail");
        ToolCallback file = tool("writeFile");
        ToolCallback search = tool("searchWebsite");
        ToolCallback[] all = new ToolCallback[]{email, file, search};
        ReflectionTestUtils.setField(service, "allTools", all);

        ToolCallback[] selected = service.selectToolsForPrompt("帮我分析一下这个需求应该怎么拆");

        assertSame(all, selected);
    }

    @Test
    void shouldSelectRemoteResearchToolSubsetForResearchPrompt() {
        ManusSessionService service = new ManusSessionService();
        ToolCallback ask = tool("AskUserQuestionTool");
        ToolCallback todo = tool("TodoWrite");
        ToolCallback research = tool("delegateResearchToOpenClaw");
        ToolCallback write = tool("writeFile");
        ToolCallback terminate = tool("doTerminate");
        ToolCallback[] all = new ToolCallback[]{ask, todo, research, write, terminate};
        ReflectionTestUtils.setField(service, "allTools", all);

        ToolCallback[] selected = service.selectToolsForPrompt("帮我调研最近 AI Agent 的趋势，并给出网页来源总结");

        assertEquals(4, selected.length);
        assertTrue(contains(selected, "AskUserQuestionTool"));
        assertTrue(contains(selected, "TodoWrite"));
        assertTrue(contains(selected, "delegateResearchToOpenClaw"));
        assertTrue(contains(selected, "doTerminate"));
    }

    @Test
    void shouldRoutePromptToExpectedDomain() {
        ManusSessionService service = new ManusSessionService();

        assertSame(ManusSessionService.TaskDomain.PPT, service.routeTaskDomain("帮我做个PPT"));
        assertSame(ManusSessionService.TaskDomain.EMAIL, service.routeTaskDomain("帮我发一封邮件"));
        assertSame(ManusSessionService.TaskDomain.PDF, service.routeTaskDomain("帮我把PDF转成图片"));
        assertSame(ManusSessionService.TaskDomain.RESEARCH, service.routeTaskDomain("帮我调研一下 AI Agent 最新进展"));
        assertSame(ManusSessionService.TaskDomain.GENERAL, service.routeTaskDomain("帮我想一个方案"));
    }

    private static boolean contains(ToolCallback[] tools, String toolName) {
        for (ToolCallback tool : tools) {
            if (toolName.equals(tool.getToolDefinition().name())) {
                return true;
            }
        }
        return false;
    }

    private static ToolCallback tool(String name) {
        ToolDefinition definition = ToolDefinition.builder()
                .name(name)
                .description("")
                .inputSchema("{}")
                .build();
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return definition;
            }

            @Override
            public String call(String toolInput) {
                return "";
            }
        };
    }
}
