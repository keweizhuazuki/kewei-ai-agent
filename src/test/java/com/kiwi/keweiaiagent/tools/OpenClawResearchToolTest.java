package com.kiwi.keweiaiagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

class OpenClawResearchToolTest {

    @Test
    void shouldReturnDelegatedResearchTextFromJsonPayload() {
        CapturingRunner runner = new CapturingRunner("""
                {
                  "status": "ok",
                  "result": {
                    "payloads": [
                      { "text": "来源1: https://example.com\\n结论: xxx", "mediaUrl": null }
                    ]
                  }
                }
                """, "", 0);
        OpenClawResearchTool tool = new OpenClawResearchTool(
                new ObjectMapper(),
                runner,
                "openclaw",
                "main",
                120,
                "spring-ai-research"
        );

        String result = tool.delegateResearchToOpenClaw("调研上海适合情侣约会的 3 个地点", "zh-CN");

        Assertions.assertTrue(result.contains("来源1: https://example.com"));
        Assertions.assertEquals("openclaw", runner.command().getFirst());
        Assertions.assertTrue(runner.command().contains("agent"));
        Assertions.assertTrue(runner.command().contains("--json"));
        Assertions.assertTrue(runner.command().contains("--agent"));
        Assertions.assertTrue(runner.command().contains("main"));
        Assertions.assertTrue(runner.command().contains("--timeout"));
        Assertions.assertTrue(runner.command().contains("120"));
        Assertions.assertTrue(runner.command().contains("--session-id"));
        Assertions.assertTrue(runner.command().stream().anyMatch(arg -> arg.startsWith("spring-ai-research-")));
        Assertions.assertTrue(runner.command().stream().anyMatch(arg -> arg.contains("必须返回中文摘要")));
    }

    @Test
    void shouldReturnErrorWhenOpenClawCommandFails() {
        OpenClawResearchTool tool = new OpenClawResearchTool(
                new ObjectMapper(),
                new CapturingRunner("", "EPERM sessions.json.lock", 1),
                "openclaw",
                "main",
                120,
                "spring-ai-research"
        );

        String result = tool.delegateResearchToOpenClaw("调研 AI Agent 趋势", "zh-CN");

        Assertions.assertTrue(result.contains("OpenClaw delegation failed"));
        Assertions.assertTrue(result.contains("EPERM sessions.json.lock"));
    }

    @Test
    void shouldReturnErrorWhenTaskMissing() {
        OpenClawResearchTool tool = new OpenClawResearchTool(
                new ObjectMapper(),
                new CapturingRunner("", "", 0),
                "openclaw",
                "main",
                120,
                "spring-ai-research"
        );

        String result = tool.delegateResearchToOpenClaw("", "zh-CN");

        Assertions.assertTrue(result.contains("research task is required"));
    }

    private static final class CapturingRunner implements OpenClawCommandRunner {
        private final OpenClawCommandRunner.CommandResult commandResult;
        private List<String> command;

        private CapturingRunner(String stdout, String stderr, int exitCode) {
            this.commandResult = new OpenClawCommandRunner.CommandResult(exitCode, stdout, stderr);
        }

        @Override
        public CommandResult run(List<String> command, Duration timeout) {
            this.command = List.copyOf(command);
            return commandResult;
        }

        private List<String> command() {
            return command;
        }
    }
}
