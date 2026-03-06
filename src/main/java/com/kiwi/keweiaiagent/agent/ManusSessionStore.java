package com.kiwi.keweiaiagent.agent;

import com.kiwi.keweiaiagent.agent.todo.TodoSnapshot;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manus 会话存储组件，负责保存会话状态、待答问题和待办快照。
 */
@Component
public class ManusSessionStore {

    /**
     * 待办快照监听器接口，用于在会话待办列表变化时接收通知。
     */
    @FunctionalInterface
    public interface TodoSnapshotListener {
        void onTodoSnapshot(TodoSnapshot todoSnapshot);
    }

    /**
     * 待回答选项对象，描述单个问题选项的展示文案和说明。
     */
    public record PendingOption(String label, String description) {}

    /**
     * 待回答问题对象，封装前端展示所需的问题元数据。
     */
    public record PendingQuestion(String id, String header, String question, Boolean multiSelect, List<PendingOption> options) {}

    /**
     * Manus 会话记录对象，聚合会话上下文、待答问题与待办状态。
     */
    public record ManusSession(
            String chatId,
            String initialPrompt,
            KeweiManus agent,
            List<AskUserQuestionTool.Question> rawPendingQuestions,
            List<PendingQuestion> pendingQuestions,
            Map<String, String> pendingAnswers,
            TodoSnapshot todoSnapshot
    ) {}

    /**
     * 当前已缓存的会话数据。
     */
    private final ConcurrentHashMap<String, ManusSession> sessions = new ConcurrentHashMap<>();
    /**
     * 会话待办快照监听器集合。
     */
    private final ConcurrentHashMap<String, List<TodoSnapshotListener>> todoSnapshotListeners = new ConcurrentHashMap<>();
    /**
     * 当前线程正在处理的会话标识。
     */
    private final ThreadLocal<String> currentSessionId = new ThreadLocal<>();

    /**
     * 保存新的会话记录。
     */
    public void putSession(String chatId, String initialPrompt, KeweiManus agent) {
        sessions.put(chatId, new ManusSession(chatId, initialPrompt, agent, null, null, null, null));
    }

    /**
     * 按会话标识读取会话记录。
     */
    public ManusSession getSession(String chatId) {
        return sessions.get(chatId);
    }

    /**
     * 移除指定会话及其关联监听器。
     */
    public void removeSession(String chatId) {
        sessions.remove(chatId);
        todoSnapshotListeners.remove(chatId);
    }

    /**
     * 保存等待用户回答的问题列表。
     */
    public void savePendingQuestions(String chatId, List<AskUserQuestionTool.Question> questions) {
        List<PendingQuestion> pendingQuestions = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            AskUserQuestionTool.Question question = questions.get(i);
            List<PendingOption> options = question.options() == null ? List.of() : question.options().stream()
                    .map(option -> new PendingOption(option.label(), option.description()))
                    .toList();
            pendingQuestions.add(new PendingQuestion(
                    "q_" + (i + 1),
                    question.header(),
                    question.question(),
                    question.multiSelect(),
                    options
            ));
        }
        sessions.computeIfPresent(chatId, (key, session) ->
                new ManusSession(key, session.initialPrompt(), session.agent(), questions, pendingQuestions, session.pendingAnswers(), session.todoSnapshot()));
    }

    /**
     * 记录用户提交的答案。
     */
    public void submitAnswers(String chatId, Map<String, String> answers) {
        sessions.computeIfPresent(chatId, (key, session) ->
                new ManusSession(key, session.initialPrompt(), session.agent(), session.rawPendingQuestions(), session.pendingQuestions(), answers, session.todoSnapshot()));
    }

    /**
     * 提取并消费用户答案，转换为原始问题对应的映射。
     */
    public Map<String, String> consumeAnswers(String chatId) {
        ManusSession session = sessions.get(chatId);
        if (session == null || session.pendingAnswers() == null) {
            return null;
        }
        sessions.put(chatId, new ManusSession(chatId, session.initialPrompt(), session.agent(), session.rawPendingQuestions(), session.pendingQuestions(), null, session.todoSnapshot()));
        if (session.pendingQuestions() == null || session.rawPendingQuestions() == null) {
            return session.pendingAnswers();
        }
        Map<String, String> mappedAnswers = new ConcurrentHashMap<>();
        for (int i = 0; i < session.pendingQuestions().size() && i < session.rawPendingQuestions().size(); i++) {
            PendingQuestion pendingQuestion = session.pendingQuestions().get(i);
            String answer = session.pendingAnswers().get(pendingQuestion.id());
            if (answer != null) {
                mappedAnswers.put(session.rawPendingQuestions().get(i).question(), answer);
            }
        }
        return mappedAnswers;
    }

    /**
     * 清空会话中的待回答问题。
     */
    public void clearPendingQuestions(String chatId) {
        sessions.computeIfPresent(chatId, (key, session) ->
                new ManusSession(key, session.initialPrompt(), session.agent(), null, null, session.pendingAnswers(), session.todoSnapshot()));
    }

    /**
     * 保存并广播当前会话的待办快照。
     */
    public void saveTodoSnapshot(String chatId, TodoSnapshot todoSnapshot) {
        sessions.computeIfPresent(chatId, (key, session) ->
                new ManusSession(key, session.initialPrompt(), session.agent(), session.rawPendingQuestions(), session.pendingQuestions(), session.pendingAnswers(), todoSnapshot));
        List<TodoSnapshotListener> listeners = todoSnapshotListeners.get(chatId);
        if (listeners != null) {
            for (TodoSnapshotListener listener : List.copyOf(listeners)) {
                listener.onTodoSnapshot(todoSnapshot);
            }
        }
    }

    public TodoSnapshot getTodoSnapshot(String chatId) {
        ManusSession session = sessions.get(chatId);
        return session == null ? null : session.todoSnapshot();
    }

    /**
     * 将指定会话设置为当前线程的活跃会话。
     */
    public void activateSession(String chatId) {
        currentSessionId.set(chatId);
    }

    /**
     * 清理当前线程记录的活跃会话。
     */
    public void clearActiveSession() {
        currentSessionId.remove();
    }

    public String currentSessionId() {
        return currentSessionId.get();
    }

    public List<PendingQuestion> getPendingQuestions(String chatId) {
        ManusSession session = sessions.get(chatId);
        return session == null ? null : session.pendingQuestions();
    }

    /**
     * 为指定会话注册待办快照监听器。
     */
    public void registerTodoSnapshotListener(String chatId, TodoSnapshotListener listener) {
        todoSnapshotListeners.compute(chatId, (key, existing) -> {
            List<TodoSnapshotListener> next = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            next.add(listener);
            return next;
        });
    }

    /**
     * 移除指定会话上的待办快照监听器。
     */
    public void unregisterTodoSnapshotListener(String chatId, TodoSnapshotListener listener) {
        todoSnapshotListeners.computeIfPresent(chatId, (key, existing) -> {
            List<TodoSnapshotListener> next = new ArrayList<>(existing);
            next.remove(listener);
            return next.isEmpty() ? null : next;
        });
    }
}
