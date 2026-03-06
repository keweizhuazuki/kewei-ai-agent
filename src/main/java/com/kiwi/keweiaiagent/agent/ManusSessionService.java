package com.kiwi.keweiaiagent.agent;

import com.kiwi.keweiaiagent.exception.BusinessException;
import com.kiwi.keweiaiagent.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Manus 会话服务，负责按任务类型选择工具并驱动会话继续执行。
 */
@Service
@Slf4j
public class ManusSessionService {

    /**
     * 任务领域枚举，描述 Manus 会话当前适配的工具集合类别。
     */
    enum TaskDomain {
        GENERAL,
        RESEARCH,
        PPT,
        PDF,
        EMAIL
    }

    /**
     * 任务领域与允许工具名称之间的映射关系。
     */
    private static final Map<TaskDomain, Set<String>> DOMAIN_TOOL_NAMES = new EnumMap<>(TaskDomain.class);

    static {
        DOMAIN_TOOL_NAMES.put(TaskDomain.GENERAL, Set.of());
        DOMAIN_TOOL_NAMES.put(TaskDomain.RESEARCH, Set.of(
                "AskUserQuestionTool",
                "TodoWrite",
                "delegateResearchToOpenClaw",
                "doTerminate"
        ));
        DOMAIN_TOOL_NAMES.put(TaskDomain.PPT, Set.of(
                "AskUserQuestionTool",
                "TodoWrite",
                "delegateResearchToOpenClaw",
                "create_pptx",
                "doTerminate"
        ));
        DOMAIN_TOOL_NAMES.put(TaskDomain.PDF, Set.of(
                "AskUserQuestionTool",
                "TodoWrite",
                "downloadResource",
                "readFile",
                "writeFile",
                "pdfToImages",
                "doTerminate"
        ));
        DOMAIN_TOOL_NAMES.put(TaskDomain.EMAIL, Set.of(
                "AskUserQuestionTool",
                "TodoWrite",
                "readFile",
                "writeFile",
                "sendEmail",
                "doTerminate"
        ));
    }

    /**
     * 系统中注册的全部工具。
     */
    @Resource
    private ToolCallback[] allTools;

    /**
     * 用于创建 Manus 智能体的大模型实例。
     */
    @Resource
    private ChatModel ollamaChatModel;

    /**
     * Manus 会话存储组件。
     */
    @Resource
    private ManusSessionStore manusSessionStore;

    /**
     * 启动新的 Manus 流式会话。
     */
    public SseEmitter startChatStream(String chatId, String message) {
        ToolCallback[] selectedTools = selectToolsForPrompt(message);
        KeweiManus manus = new KeweiManus(selectedTools, ollamaChatModel);
        manus.setSessionId(chatId);
        manus.setManusSessionStore(manusSessionStore);
        manusSessionStore.putSession(chatId, message, manus);
        return manus.runStream(message);
    }

    /**
     * 基于补充答案继续执行 Manus 会话。
     */
    public SseEmitter continueChatStream(String chatId, Map<String, String> answers) {
        ManusSessionStore.ManusSession session = manusSessionStore.getSession(chatId);
        if (session == null || session.agent() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "未找到待继续的会话");
        }
        String followupPrompt = buildFollowupPrompt(session, answers);

        ToolCallback[] selectedTools = selectToolsForPrompt(session.initialPrompt());
        KeweiManus manus = new KeweiManus(selectedTools, ollamaChatModel);
        manus.setSessionId(chatId);
        manus.setManusSessionStore(manusSessionStore);
        manusSessionStore.putSession(chatId, followupPrompt, manus);
        return manus.runStream(followupPrompt);
    }

    ToolCallback[] selectToolsForPrompt(String prompt) {
        TaskDomain domain = routeTaskDomain(prompt);
        if (domain == TaskDomain.GENERAL) {
            return allTools;
        }
        Set<String> allowedTools = DOMAIN_TOOL_NAMES.getOrDefault(domain, Set.of());
        ToolCallback[] selected = Arrays.stream(allTools)
                .filter(tool -> allowedTools.contains(tool.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);
        if (selected.length == 0) {
            log.warn("No tools matched for domain {}; falling back to all tools", domain);
            return allTools;
        }
        Set<String> selectedNames = new LinkedHashSet<>();
        for (ToolCallback tool : selected) {
            selectedNames.add(tool.getToolDefinition().name());
        }
        log.info("Selected Manus tool subset for domain {}: {}", domain, selectedNames);
        return selected;
    }

    TaskDomain routeTaskDomain(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return TaskDomain.GENERAL;
        }
        String normalized = prompt.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "ppt", "幻灯片", "演示文稿")) {
            return TaskDomain.PPT;
        }
        if (containsAny(normalized, "research", "调研", "搜集资料", "网页来源", "资料收集", "研究一下")) {
            return TaskDomain.RESEARCH;
        }
        if (containsAny(normalized, "pdf", "表单", "填写pdf", "填充pdf", "转换pdf", "convert pdf")) {
            return TaskDomain.PDF;
        }
        if (containsAny(normalized, "email", "mail", "邮件", "发信", "发送邮箱", "发送邮件")) {
            return TaskDomain.EMAIL;
        }
        return TaskDomain.GENERAL;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将用户补充答案整理为可继续执行的跟进提示词。
     */
    private String buildFollowupPrompt(ManusSessionStore.ManusSession session, Map<String, String> answers) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("原始任务：\n").append(session.initialPrompt()).append("\n\n");
        prompt.append("用户补充信息如下：\n");
        if (session.pendingQuestions() != null) {
            for (ManusSessionStore.PendingQuestion question : session.pendingQuestions()) {
                String answer = answers.get(question.id());
                if (answer == null || answer.isBlank()) {
                    continue;
                }
                prompt.append("- ").append(question.question()).append("：").append(answer).append("\n");
            }
        }
        prompt.append("\n请基于以上已确认信息继续执行任务，不要重复提问；只有在确实缺少完成任务所必需的信息时，才再次调用 AskUserQuestionTool。");
        return prompt.toString();
    }
}
