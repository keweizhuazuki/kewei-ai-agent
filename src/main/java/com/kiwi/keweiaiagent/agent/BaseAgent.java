package com.kiwi.keweiaiagent.agent;


import com.kiwi.keweiaiagent.agent.model.AgentState;
import com.kiwi.keweiaiagent.agent.todo.TodoSnapshot;
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
 * 智能体抽象基类，封装状态管理、执行循环和流式事件分发等通用能力。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    /**
     * 智能体名称，用于日志输出和执行结果标识。
     */
    private String name;

    /**
     * 系统提示词，用于约束智能体的全局行为。
     */
    private String systemPrompt;

    /**
     * 下一步提示词，用于驱动每轮执行后的继续推理。
     */
    private String nextStepPrompt;

    /**
     * 当前智能体状态。
     */
    private AgentState state = AgentState.IDLE;

    /**
     * 当前执行到的步骤序号。
     */
    private int currentStep = 0;

    /**
     * 允许执行的最大步骤数。
     */
    private int maxSteps = 10;

    /**
     * 关联的会话标识，用于流式场景下恢复上下文。
     */
    private String sessionId;

    /**
     * 会话存储组件，用于同步问题与待办快照。
     */
    private ManusSessionStore manusSessionStore;

    // llm 大模型
    /**
     * 底层大模型客户端。
     */
    private ChatClient chatClient;

    // Memory 记忆模块
    /**
     * 当前会话内维护的消息历史。
     */
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

    /**
     * 以 SSE 方式启动智能体执行，并持续向前端推送过程事件。
     */
    public SseEmitter runStream(String userPrompt){
        validatePromptAndState(userPrompt);
        SseEmitter sseEmitter = new SseEmitter(300000L);
        this.state = AgentState.RUNNING;
        messageList.add(new UserMessage(userPrompt));

        return executeStreamLoop(sseEmitter, false);
    }

    /**
     * 在补充完用户输入后继续流式执行未完成的会话。
     */
    public SseEmitter resumeStream() {
        validateResumeState();
        SseEmitter sseEmitter = new SseEmitter(300000L);
        this.state = AgentState.RUNNING;
        return executeStreamLoop(sseEmitter, true);
    }

    /**
     * 执行流式循环并在过程中分发问题、待办和错误事件。
     */
    private SseEmitter executeStreamLoop(SseEmitter sseEmitter, boolean resumePendingStep) {
        CompletableFuture.runAsync(() -> {
            boolean activateSession = manusSessionStore != null && StringUtils.hasText(sessionId);
            ManusSessionStore.TodoSnapshotListener todoListener = null;
            try {
                if (activateSession) {
                    manusSessionStore.activateSession(sessionId);
                    todoListener = snapshot -> sendTodoEvent(sseEmitter, snapshot);
                    manusSessionStore.registerTodoSnapshotListener(sessionId, todoListener);
                    TodoSnapshot existingSnapshot = manusSessionStore.getTodoSnapshot(sessionId);
                    if (existingSnapshot != null) {
                        sendTodoEvent(sseEmitter, existingSnapshot);
                    }
                }
                for(int i = 0; i < maxSteps && state != AgentState.FINISHED; i++){
                    currentStep = i + 1;
                    log.info("Agent {} executing step {}/{}", name, currentStep, maxSteps);
                    String stepResult = (resumePendingStep && i == 0) ? resumeStep() : step();
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
            } catch (PendingUserQuestionException e) {
                state = AgentState.WAITING_FOR_USER_INPUT;
                sendQuestionEvent(sseEmitter, e);
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
                if (activateSession && todoListener != null) {
                    manusSessionStore.unregisterTodoSnapshotListener(sessionId, todoListener);
                }
                if (activateSession) {
                    manusSessionStore.clearActiveSession();
                }
                if (state != AgentState.WAITING_FOR_USER_INPUT) {
                    cleanup();
                }
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
            if (state != AgentState.WAITING_FOR_USER_INPUT) {
                this.cleanup();
            }
                log.info("Agent {} runStream completed with state {}", name, state);
        });

        return sseEmitter;
    }

    /**
     * 校验输入提示词和当前智能体状态是否合法。
     */
    private void validatePromptAndState(String userPrompt) {
        if(this.state != AgentState.IDLE){
            throw new BusinessException(ErrorCode.AGENT_BUSY);
        }
        if (!StringUtils.hasText(userPrompt)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "userPrompt不能为空");
        }
    }

    /**
     * 校验当前智能体是否允许继续执行。
     */
    private void validateResumeState() {
        if (this.state != AgentState.WAITING_FOR_USER_INPUT) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "当前会话不处于待回答状态");
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

    /**
     * 向前端推送待回答问题事件。
     */
    private void sendQuestionEvent(SseEmitter sseEmitter, PendingUserQuestionException exception) {
        try {
            Object payload = exception.getQuestions();
            if (manusSessionStore != null && StringUtils.hasText(sessionId)) {
                payload = new QuestionEventPayload(manusSessionStore.getPendingQuestions(sessionId));
            }
            sseEmitter.send(SseEmitter.event().name("question").data(payload));
        } catch (IOException ioException) {
            log.warn("Agent {} failed to send question event: {}", name, ioException.getMessage(), ioException);
        }
    }

    /**
     * 向前端推送待办快照事件。
     */
    private void sendTodoEvent(SseEmitter sseEmitter, TodoSnapshot snapshot) {
        try {
            TodoEventPayload payload = new TodoEventPayload(snapshot);
            if (payload != null) {
                sseEmitter.send(SseEmitter.event().name("todo").data(payload));
            }
        } catch (IOException ioException) {
            log.warn("Agent {} failed to send todo event: {}", name, ioException.getMessage(), ioException);
        }
    }

    TodoEventPayload buildTodoEventPayload() {
        if (manusSessionStore == null || !StringUtils.hasText(sessionId)) {
            return null;
        }
        TodoSnapshot snapshot = manusSessionStore.getTodoSnapshot(sessionId);
        if (snapshot == null) {
            return null;
        }
        return new TodoEventPayload(snapshot);
    }

    /**
     * 执行单步逻辑，交由子类给出具体实现。
     */
    public abstract String step();

    /**
     * 恢复未完成步骤，默认复用单步执行逻辑。
     */
    protected String resumeStep() {
        return step();
    }

    /**
     * 执行结束后的清理钩子，供子类按需扩展。
     */
    protected void cleanup(){
        // 清理资源，重置状态等
        if ((state == AgentState.FINISHED || state == AgentState.ERROR)
                && manusSessionStore != null
                && StringUtils.hasText(sessionId)) {
            manusSessionStore.removeSession(sessionId);
        }
    }

    /**
     * 待回答问题事件载荷，封装需要推送给前端的问题内容。
     */
    public record QuestionEventPayload(Object questions) {}

    /**
     * 待办快照事件载荷，封装当前任务清单的推送数据。
     */
    public record TodoEventPayload(TodoSnapshot todo) {}

}
