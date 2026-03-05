package com.kiwi.keweiaiagent.agent;


import com.kiwi.keweiaiagent.agent.model.AgentState;
import com.kiwi.keweiaiagent.exception.BusinessException;
import com.kiwi.keweiaiagent.exception.ErrorCode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象类，定义了智能体的基本属性和方法
 */

@Data
@Slf4j
public abstract class BaseAgent {

    private String name;

    private String systemPrompt;

    private String nextStepPrompt;

    private AgentState state = AgentState.IDLE;

    private int currentStep = 0;

    private int maxSteps = 10;

    // llm 大模型
    private ChatClient chatClient;

    // Memory 记忆模块
    private List<Message> messageList = new ArrayList<>();
    /**
     * 执行智能体的主要运行逻辑。
     *
     * @param userPrompt 用户输入的提示信息，不能为空。
     * @return 返回运行过程中每一步的结果，按行分隔。
     * @throws BusinessException 如果智能体当前状态不是空闲状态，抛出 AGENT_BUSY 异常；
     *                           如果用户输入为空，抛出 INVALID_PARAM 异常。
     */
    public String run(String userPrompt){
        validatePromptAndState(userPrompt);

        // 设置智能体状态为运行中
        this.state = AgentState.RUNNING;
        messageList.add(new UserMessage(userPrompt));

        // 执行循环逻辑
        List<String> results = new ArrayList<>();
        try {
            for(int i = 0; i < maxSteps && state != AgentState.FINISHED; i++){
                currentStep = i + 1;
                log.info("Agent {} executing step {}/{}", name, currentStep, maxSteps);
                String stepResult = step();
                String result = String.format("Step %d result: %s", currentStep, stepResult);
                results.add(result);
            }
            // 如果达到最大步数，设置状态为完成
            if(currentStep >= maxSteps){
                state = AgentState.FINISHED;
                results.add(String.format("Agent {} reached max steps", name));
            }
        } catch (BusinessException e) {
            // 捕获异常并设置状态为错误
            state = AgentState.ERROR;
            log.error(e.getMessage());
            return String.format("Agent %s error: %s", name, e.getMessage());
        } finally {
            // 清理资源
            cleanup();
        }

        // 返回所有步骤的结果
        return String.join("\n", results);
    }

    public SseEmitter runStream(String userPrompt){
        validatePromptAndState(userPrompt);
        SseEmitter sseEmitter = new SseEmitter(300000L);
        this.state = AgentState.RUNNING;
        messageList.add(new UserMessage(userPrompt));

        CompletableFuture.runAsync(() -> {
            try {
                for(int i = 0; i < maxSteps && state != AgentState.FINISHED; i++){
                    currentStep = i + 1;
                    log.info("Agent {} executing step {}/{}", name, currentStep, maxSteps);
                    String stepResult = step();
                    String result = String.format("Step %d result: %s", currentStep, stepResult);
                    sseEmitter.send(SseEmitter.event().name("message").data(result));
                }
                // 如果达到最大步数，设置状态为完成
                if(currentStep >= maxSteps && state != AgentState.FINISHED){
                    state = AgentState.FINISHED;
                    sseEmitter.send(SseEmitter.event()
                            .name("message")
                            .data(String.format("Agent %s reached max steps", name)));
                }
                sseEmitter.send(SseEmitter.event().name("done").data("[DONE]"));
                sseEmitter.complete();
            } catch (BusinessException e) {
                state = AgentState.ERROR;
                log.error("Agent {} runStream failed: {}", name, e.getMessage(), e);
                sendErrorEvent(sseEmitter, formatErrorMessage(e));
                sseEmitter.completeWithError(e);
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("Agent {} runStream unexpected error", name, e);
                sendErrorEvent(sseEmitter, String.format("Agent %s error: %s", name, e.getMessage()));
                sseEmitter.completeWithError(e);
            } finally {
                cleanup();
            }
        });

        sseEmitter.onTimeout(()->{
            this.state = AgentState.ERROR;
            this.cleanup();
                log.warn("Agent {} runStream timed out", name);
                sendErrorEvent(sseEmitter, String.format("Agent %s error: runStream timed out", name));
        });

        sseEmitter.onCompletion(()->{
            if(state == AgentState.RUNNING){
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
                log.info("Agent {} runStream completed with state {}", name, state);
        });

        return sseEmitter;
    }

    private void validatePromptAndState(String userPrompt) {
        if(this.state != AgentState.IDLE){
            throw new BusinessException(ErrorCode.AGENT_BUSY);
        }
        if (!StringUtils.hasText(userPrompt)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "userPrompt不能为空");
        }
    }

    private String formatErrorMessage(BusinessException e) {
        return String.format("Agent %s error: %s", name, e.getMessage());
    }

    private void sendErrorEvent(SseEmitter sseEmitter, String errorMessage) {
        try {
            sseEmitter.send(SseEmitter.event().name("error").data(errorMessage));
        } catch (IOException ioException) {
            log.warn("Agent {} failed to send error event: {}", name, ioException.getMessage(), ioException);
        }
    }

    public abstract String step();

    protected void cleanup(){
        // 清理资源，重置状态等
    }

}
