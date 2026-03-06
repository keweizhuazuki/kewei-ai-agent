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

@Service
@Slf4j
public class ManusSessionService {

    enum TaskDomain {
        GENERAL,
        PPT,
        PDF,
        EMAIL
    }

    private static final Map<TaskDomain, Set<String>> DOMAIN_TOOL_NAMES = new EnumMap<>(TaskDomain.class);

    static {
        DOMAIN_TOOL_NAMES.put(TaskDomain.GENERAL, Set.of());
        DOMAIN_TOOL_NAMES.put(TaskDomain.PPT, Set.of(
                "AskUserQuestionTool",
                "searchWebsite",
                "scrapeWebsite",
                "create_pptx",
                "doTerminate"
        ));
        DOMAIN_TOOL_NAMES.put(TaskDomain.PDF, Set.of(
                "AskUserQuestionTool",
                "downloadResource",
                "readFile",
                "writeFile",
                "pdfToImages",
                "doTerminate"
        ));
        DOMAIN_TOOL_NAMES.put(TaskDomain.EMAIL, Set.of(
                "AskUserQuestionTool",
                "readFile",
                "writeFile",
                "sendEmail",
                "doTerminate"
        ));
    }

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel ollamaChatModel;

    @Resource
    private ManusSessionStore manusSessionStore;

    public SseEmitter startChatStream(String chatId, String message) {
        ToolCallback[] selectedTools = selectToolsForPrompt(message);
        KeweiManus manus = new KeweiManus(selectedTools, ollamaChatModel);
        manus.setSessionId(chatId);
        manus.setManusSessionStore(manusSessionStore);
        manusSessionStore.putSession(chatId, message, manus);
        return manus.runStream(message);
    }

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
