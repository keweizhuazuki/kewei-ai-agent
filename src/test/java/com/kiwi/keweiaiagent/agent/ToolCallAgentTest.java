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
import org.springframework.ai.tool.definition.ToolDefinition;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallAgentTest {

    @Test
    void act_shouldDispatchRegisteredCallbackWithoutCallingModel() {
        AtomicInteger calls = new AtomicInteger();
        ToolCallback callback = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("offlineFixture")
                        .description("Synthetic deterministic callback")
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                        .build();
            }

            @Override
            public String call(String arguments) {
                calls.incrementAndGet();
                return "fixture result";
            }
        };
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[] { callback });
        agent.getMessageList().add(new UserMessage("Synthetic task"));
        AssistantMessage plan = AssistantMessage.builder().content("")
                .toolCalls(List.of(new ToolCall("fixture", "function", "offlineFixture", "{}")))
                .build();
        agent.setToolCallChatResponse(new ChatResponse(List.of(new Generation(plan))));

        assertTrue(agent.act().contains("fixture result"));
        assertEquals(1, calls.get());
        ToolResponseMessage response = (ToolResponseMessage) agent.getMessageList().getLast();
        assertEquals("fixture result", response.getResponses().getFirst().responseData());
    }

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

    @Test
    void shouldDetectPendingUserInputFromAskUserQuestionToolResponse() {
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[0]);
        ToolResponseMessage message = ToolResponseMessage.builder()
                .responses(List.of(new ToolResponseMessage.ToolResponse("1", "AskUserQuestionTool", "Pending user input")))
                .build();

        assertTrue(agent.hasPendingUserInput(message));
    }
}
