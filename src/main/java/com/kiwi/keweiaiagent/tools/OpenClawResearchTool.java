package com.kiwi.keweiaiagent.tools;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class OpenClawResearchTool {

    private final ObjectMapper objectMapper;
    private final OpenClawCommandRunner commandRunner;
    private final String command;
    private final String agentId;
    private final Duration timeout;
    private final String sessionPrefix;

    public OpenClawResearchTool(
            ObjectMapper objectMapper,
            OpenClawCommandRunner commandRunner,
            @Value("${openclaw.agent.command:openclaw}") String command,
            @Value("${openclaw.agent.id:main}") String agentId,
            @Value("${openclaw.agent.timeout-seconds:120}") long timeoutSeconds,
            @Value("${openclaw.agent.session-prefix:spring-ai-research}") String sessionPrefix
    ) {
        this.objectMapper = objectMapper;
        this.commandRunner = commandRunner;
        this.command = command;
        this.agentId = agentId;
        this.timeout = Duration.ofSeconds(Math.max(10, timeoutSeconds));
        this.sessionPrefix = sessionPrefix;
    }

    @Tool(description = "Delegate web research to the remote OpenClaw execution agent and return a structured handoff summary", returnDirect = false)
    public String delegateResearchToOpenClaw(
            @ToolParam(description = "Research goal or question that needs web search and page collection") String task,
            @ToolParam(description = "Optional locale for search output, e.g. zh-CN or en-US") String locale
    ) {
        if (StrUtil.isBlank(task)) {
            return "Error: research task is required.";
        }
        String normalizedLocale = StrUtil.isBlank(locale) ? "zh-CN" : locale.trim();
        List<String> command = buildCommand(task.trim(), normalizedLocale);
        log.info("Delegating research to OpenClaw. task={}, locale={}, agentId={}, command={}",
                task.trim(), normalizedLocale, agentId, command);
        try {
            OpenClawCommandRunner.CommandResult result = commandRunner.run(command, timeout);
            if (result.exitCode() != 0) {
                log.warn("OpenClaw delegation failed. exitCode={}, stderrPreview={}, stdoutPreview={}",
                        result.exitCode(),
                        preview(result.stderr()),
                        preview(result.stdout()));
                return "OpenClaw delegation failed: " + firstNonBlank(result.stderr(), result.stdout(), "unknown error");
            }
            String researchText = extractResearchText(result.stdout());
            log.info("OpenClaw delegation succeeded. responsePreview={}", preview(researchText));
            return researchText;
        } catch (IOException e) {
            log.warn("OpenClaw delegation failed with IO exception", e);
            return "OpenClaw delegation failed: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("OpenClaw delegation interrupted", e);
            return "OpenClaw delegation interrupted.";
        }
    }

    List<String> buildCommand(String task, String locale) {
        List<String> commandParts = new ArrayList<>();
        commandParts.add(command);
        commandParts.add("agent");
        if (StrUtil.isNotBlank(agentId)) {
            commandParts.add("--agent");
            commandParts.add(agentId);
        }
        commandParts.add("--message");
        commandParts.add(buildResearchPrompt(task, locale));
        commandParts.add("--json");
        commandParts.add("--timeout");
        commandParts.add(String.valueOf(timeout.toSeconds()));
        commandParts.add("--session-id");
        commandParts.add(buildSessionId());
        return commandParts;
    }

    String buildResearchPrompt(String task, String locale) {
        return """
                你是 OpenClaw 调研执行代理。请围绕下面的目标执行网页调研，并只返回最终调研结果，不要输出你的思考过程。

                调研目标：%s
                输出语言：%s

                执行要求：
                1. 先搜索并打开最相关的网页来源。
                2. 结果必须返回中文摘要。
                3. 至少给出 3 条发现。
                4. 每条发现都要附带对应来源 URL。
                5. 如果信息不足，明确说明缺口，不要编造。
                """.formatted(task, locale);
    }

    private String buildSessionId() {
        String prefix = StrUtil.blankToDefault(sessionPrefix, "spring-ai-research")
                .trim()
                .replaceAll("[^a-zA-Z0-9-_]", "-");
        return prefix + "-" + UUID.randomUUID();
    }

    private String extractResearchText(String stdout) throws IOException {
        if (StrUtil.isBlank(stdout)) {
            return "OpenClaw delegation failed: empty response.";
        }
        JsonNode root = objectMapper.readTree(stdout);
        JsonNode payloads = root.path("result").path("payloads");
        if (payloads.isArray() && !payloads.isEmpty()) {
            List<String> texts = new ArrayList<>();
            for (JsonNode payload : payloads) {
                String text = payload.path("text").asText("");
                if (StrUtil.isNotBlank(text)) {
                    texts.add(text.trim());
                }
            }
            if (!texts.isEmpty()) {
                return String.join("\n\n", texts);
            }
        }
        String summary = root.path("summary").asText("");
        if (StrUtil.isNotBlank(summary)) {
            return summary;
        }
        return stdout;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String preview(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 400 ? normalized : normalized.substring(0, 400) + "...";
    }
}
