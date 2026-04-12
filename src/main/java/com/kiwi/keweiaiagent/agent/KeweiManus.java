package com.kiwi.keweiaiagent.agent;

import com.kiwi.keweiaiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;


/**
 * Manus 智能体实现类，负责装配工具集、系统提示词和大模型客户端。
 */
@Component
public class KeweiManus extends ToolCallAgent{
    public KeweiManus(ToolCallback[] allTools, ChatModel ollamaChatModel) {
        this(allTools, ollamaChatModel, "");
    }

    public KeweiManus(ToolCallback[] allTools, ChatModel ollamaChatModel, String longTermMemoryPrompt) {
        super(allTools);
        this.setName("KeweiManus");
        String System_Prompt = """
                You are KeweiManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                For any multi-step task, first call TodoWrite to create a concise plan before executing.
                Keep the todo list updated as work moves from pending to in_progress to completed.
                If a task will be delegated through delegateResearchToOpenClaw, treat that delegation as one atomic todo step.
                Do not split the remote OpenClaw work into fake internal subtasks like searching, scraping, and pricing inside TodoWrite.
                                
                For durable facts that should survive across sessions, use the long-term memory tools and keep MEMORY.md in sync.
                """ + "\n\n" + longTermMemoryPrompt;
        this.setSystemPrompt(System_Prompt);

        String Next_Step_Prompt = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                Whenever you complete a subtask or move to the next subtask, call TodoWrite immediately to refresh the checklist before using another tool.
                When delegateResearchToOpenClaw is the chosen tool, keep the todo list at the orchestration level only, for example: clarify goal, delegate research, summarize result.
                Reuse long-term memory only for durable, cross-session facts, and avoid saving transient task noise.
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
