package com.kiwi.keweiaiagent.agent;

import org.springaicommunity.agent.tools.AskUserQuestionTool;

import java.util.List;

public class PendingUserQuestionException extends RuntimeException {

    private final List<AskUserQuestionTool.Question> questions;

    public PendingUserQuestionException(List<AskUserQuestionTool.Question> questions) {
        super("Pending user input");
        this.questions = questions;
    }

    public List<AskUserQuestionTool.Question> getQuestions() {
        return questions;
    }
}
