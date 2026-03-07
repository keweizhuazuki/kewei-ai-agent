package com.kiwi.keweiaiagent.agent;

import com.kiwi.keweiaiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;


@Component
public class KeweiManus extends ToolCallAgent{
    public KeweiManus(ToolCallback[] allTools, ChatModel ollamaChatModel) {
        super(allTools);
        this.setName("KeweiManus");
        String System_Prompt = """
                You are KeweiManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                For any multi-step task, first call TodoWrite to create a concise plan before executing.
                Keep the todo list updated as work moves from pending to in_progress to completed.
                """;
        this.setSystemPrompt(System_Prompt);

        String Next_Step_Prompt = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                Whenever you complete a subtask or move to the next subtask, call TodoWrite immediately to refresh the checklist before using another tool.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If you want to stop the interaction at any point, use the terminate’ tool/function call.
                """;
        this.setNextStepPrompt(Next_Step_Prompt);
        this.setMaxSteps(20);

        ChatClient chatClient = ChatClient.builder(ollamaChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
