package com.kiwi.keweiaiagent.tools;

import com.kiwi.keweiaiagent.agent.ManusSessionStore;
import com.kiwi.keweiaiagent.agent.PendingUserQuestionException;
import jakarta.annotation.Resource;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 提问工具，负责将需要用户补充的信息转成结构化问题。
 */
@Component
public class AskQuestionTool {

    @Resource
    private ManusSessionStore manusSessionStore;

    @Bean
    public AskUserQuestionTool askUserQuestionTool() {
        return AskUserQuestionTool.builder()
                .questionHandler(this::handleQuestions)
                .build();
    }

    private Map<String, String> handleQuestions(List<AskUserQuestionTool.Question> questions) {
        String sessionId = manusSessionStore.currentSessionId();
        if (sessionId == null) {
            throw new IllegalStateException("AskUserQuestionTool called without active Manus session");
        }
        Map<String, String> answers = manusSessionStore.consumeAnswers(sessionId);
        if (answers != null && !answers.isEmpty()) {
            manusSessionStore.clearPendingQuestions(sessionId);
            return new HashMap<>(answers);
        }
        manusSessionStore.savePendingQuestions(sessionId, questions);
        throw new PendingUserQuestionException(questions);
    }
}
