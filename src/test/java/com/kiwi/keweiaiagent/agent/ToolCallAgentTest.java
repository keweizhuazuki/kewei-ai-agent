package com.kiwi.keweiaiagent.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallAgentTest {

    @Test
    void think_shouldNotAppendNextStepPromptAsUserMessageOnEveryStep() {
        ChatModel fakeChatModel = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
            }
        };
        ChatClient chatClient = ChatClient.builder(fakeChatModel).build();

        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[0]);
        agent.setChatClient(chatClient);
        agent.setSystemPrompt("system");
        agent.setNextStepPrompt("next-step");
        agent.getMessageList().add(new UserMessage("user-task"));

        assertFalse(agent.think());
        assertFalse(agent.think());

        long userMessageCount = agent.getMessageList().stream()
                .filter(message -> message instanceof UserMessage)
                .count();
        assertEquals(1, userMessageCount);
    }

    @Test
    void summarizeToolPlan_shouldIncludeToolNamesAndResultHint() {
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[0]);
        AssistantMessage message = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(
                        new ToolCall("1", "function", "searchWebsite", "{\"q\":\"x\"}"),
                        new ToolCall("2", "function", "scrapeWebsite", "{\"url\":\"u\"}")
                ))
                .build();

        String summary = agent.summarizeToolPlan(message);
        assertTrue(summary.contains("searchWebsite"));
        assertTrue(summary.contains("scrapeWebsite"));
        assertTrue(summary.contains("完成后会汇总结果"));
    }
}
