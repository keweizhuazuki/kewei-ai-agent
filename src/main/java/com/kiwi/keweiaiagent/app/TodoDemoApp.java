package com.kiwi.keweiaiagent.app;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Set;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Component
public class TodoDemoApp {

    private static final String SYSTEM_PROMPT = """
            You are a TodoWrite demo assistant.
            For tasks with more than a couple of steps, first call TodoWrite to create a short plan.
            Then keep the todo list updated while working.
            Use doTerminate when the task is complete.
            """;

    private static final Set<String> DEMO_TOOL_NAMES = Set.of(
            "TodoWrite",
            "AskUserQuestionTool",
            "doTerminate"
    );

    private final ChatClient chatClient;
    private final ToolCallback[] demoTools;

    public TodoDemoApp(ChatModel ollamaChatModel, ToolCallback[] allTools) {
        this.chatClient = ChatClient.builder(ollamaChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
        this.demoTools = Arrays.stream(allTools)
                .filter(tool -> DEMO_TOOL_NAMES.contains(tool.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);
    }

    public String call(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(CONVERSATION_ID, chatId))
                .toolCallbacks(demoTools)
                .call()
                .chatResponse();
        assert chatResponse != null;
        return chatResponse.getResult().getOutput().getText();
    }

    public Flux<String> stream(String message, String chatId) {
        return Flux.defer(() -> Flux.just(call(message, chatId)));
    }
}
