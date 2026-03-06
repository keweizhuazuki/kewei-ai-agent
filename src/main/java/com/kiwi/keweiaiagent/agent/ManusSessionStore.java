package com.kiwi.keweiaiagent.agent;

import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ManusSessionStore {

    public record PendingOption(String label, String description) {}

    public record PendingQuestion(String id, String header, String question, Boolean multiSelect, List<PendingOption> options) {}

    public record ManusSession(
            String chatId,
            String initialPrompt,
            KeweiManus agent,
            List<AskUserQuestionTool.Question> rawPendingQuestions,
            List<PendingQuestion> pendingQuestions,
            Map<String, String> pendingAnswers
    ) {}

    private final ConcurrentHashMap<String, ManusSession> sessions = new ConcurrentHashMap<>();
    private final ThreadLocal<String> currentSessionId = new ThreadLocal<>();

    public void putSession(String chatId, String initialPrompt, KeweiManus agent) {
        sessions.put(chatId, new ManusSession(chatId, initialPrompt, agent, null, null, null));
    }

    public ManusSession getSession(String chatId) {
        return sessions.get(chatId);
    }

    public void removeSession(String chatId) {
        sessions.remove(chatId);
    }

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
                new ManusSession(key, session.initialPrompt(), session.agent(), questions, pendingQuestions, session.pendingAnswers()));
    }

    public void submitAnswers(String chatId, Map<String, String> answers) {
        sessions.computeIfPresent(chatId, (key, session) ->
                new ManusSession(key, session.initialPrompt(), session.agent(), session.rawPendingQuestions(), session.pendingQuestions(), answers));
    }

    public Map<String, String> consumeAnswers(String chatId) {
        ManusSession session = sessions.get(chatId);
        if (session == null || session.pendingAnswers() == null) {
            return null;
        }
        sessions.put(chatId, new ManusSession(chatId, session.initialPrompt(), session.agent(), session.rawPendingQuestions(), session.pendingQuestions(), null));
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

    public void clearPendingQuestions(String chatId) {
        sessions.computeIfPresent(chatId, (key, session) ->
                new ManusSession(key, session.initialPrompt(), session.agent(), null, null, session.pendingAnswers()));
    }

    public void activateSession(String chatId) {
        currentSessionId.set(chatId);
    }

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
}
