package com.kiwi.keweiaiagent.agent;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.kiwi.keweiaiagent.agent.model.AgentState;
import com.kiwi.keweiaiagent.exception.BusinessException;
import com.kiwi.keweiaiagent.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent{

    private final ToolCallback[] availableTools;

    private ChatResponse toolCallChatResponse;

    private final ToolCallingManager toolCallingManager;

    private final ChatOptions chatOptions;
    private String latestAssistantText;

    public ToolCallAgent(ToolCallback[] toolCalbacks){
        super();
        this.availableTools = toolCalbacks;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 配置工具调用选项，禁用内部工具执行
        this.chatOptions = ToolCallingChatOptions
                .builder()
                .internalToolExecutionEnabled(false)
                .build();
    }

    @Override
    public boolean think() {
        try {
//            if(StrUtil.isNotBlank(getNextStepPrompt())){
//                UserMessage userMessage = new UserMessage(getNextStepPrompt());
//                getMessageList().add(userMessage);
//            }
            // 1: 调用 AI 大模型，获取工具调用结果
            List<Message> messageList = getMessageList();
            Prompt prompt = new Prompt(messageList, this.chatOptions);

            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(buildSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();
            // 记录 Response 响应，用于等下的 act() 方法执行工具调用
            this.toolCallChatResponse = chatResponse;
            // 2: 解析工具调用结果，获取要调用的工具
            // 助手消息
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 获取要调用的工具列表
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // 输出提示信息
            String result = assistantMessage.getText();
            if (StrUtil.isBlank(result) && !toolCallList.isEmpty()) {
                result = summarizeToolPlan(assistantMessage);
            }
            this.latestAssistantText = StrUtil.blankToDefault(result, "");
            log.info("工具调用结果: {}, 要调用的工具列表: {}", result, toolCallList);
            //如果不需要调用工具，返回 false
            if(toolCallList.isEmpty()) {
                getMessageList().add(assistantMessage);
                setState(AgentState.FINISHED);
                log.info("不需要调用工具，思考结束");
                return false;
            }
            return true;
        } catch (BusinessException e) {
            log.error("工具调用过程中发生错误: {}", e.getMessage());
            getMessageList().add(new AssistantMessage(e.getMessage()));
            return false;
        } finally {
            cleanup();
        }
    }

    @Override
    public String act() {
        if(!toolCallChatResponse.hasToolCalls()){
            return "没有工具调用请求，无法执行行动";
        }

        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);

        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);

        setMessageList(toolExecutionResult.conversationHistory());

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());

        if (hasPendingUserInput(toolResponseMessage)) {
            throw new PendingUserQuestionException(List.of());
        }

        // 判断是否调用终止工具
        boolean doTerminate = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if(doTerminate){
            log.info("检测到终止工具调用，行动结束");
            setState(AgentState.FINISHED);
        }
        String results = toolResponseMessage
                .getResponses()
                .stream()
                .map(response -> "工具" + response.name() + "返回结果：" + response.responseData())
                .collect(Collectors.joining("\n"));
        if (StrUtil.isNotBlank(latestAssistantText)) {
            results = results + "\n\nFinal Answer: " + latestAssistantText;
        }
        log.info(results);
        return results;
    }

    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (shouldAct) {
                return act();
            }
            if (StrUtil.isNotBlank(latestAssistantText)) {
                return "Final Answer: " + latestAssistantText;
            }
            return "思考结束，不需要行动";
        } catch (BusinessException e) {
            return "执行过程中发生错误: " + e.getMessage();
        }
    }

    @Override
    protected String resumeStep() {
        if (toolCallChatResponse != null && toolCallChatResponse.hasToolCalls()) {
            return act();
        }
        return step();
    }

    private String buildSystemPrompt() {
        String systemPrompt = StrUtil.blankToDefault(getSystemPrompt(), "");
        String nextStepPrompt = StrUtil.blankToDefault(getNextStepPrompt(), "");
        if (StrUtil.isBlank(systemPrompt)) {
            return nextStepPrompt;
        }
        if (StrUtil.isBlank(nextStepPrompt)) {
            return systemPrompt;
        }
        return systemPrompt + "\n\n" + nextStepPrompt;
    }

    String summarizeToolPlan(AssistantMessage assistantMessage) {
        List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
        if (CollUtil.isEmpty(toolCalls)) {
            return StrUtil.blankToDefault(assistantMessage.getText(), "");
        }
        String tools = toolCalls.stream()
                .map(AssistantMessage.ToolCall::name)
                .collect(Collectors.joining(" -> "));
        return "计划执行工具链: " + tools + "；完成后会汇总结果。";
    }

    boolean hasPendingUserInput(ToolResponseMessage toolResponseMessage) {
        if (toolResponseMessage == null || CollUtil.isEmpty(toolResponseMessage.getResponses())) {
            return false;
        }
        return toolResponseMessage.getResponses().stream().anyMatch(response ->
                "AskUserQuestionTool".equals(response.name())
                        && StrUtil.containsIgnoreCase(StrUtil.blankToDefault(response.responseData(), ""), "pending user input"));
    }
}
