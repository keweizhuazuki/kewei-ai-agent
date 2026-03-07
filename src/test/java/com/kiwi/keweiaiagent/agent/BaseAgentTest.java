package com.kiwi.keweiaiagent.agent;

import com.kiwi.keweiaiagent.agent.model.AgentState;
import com.kiwi.keweiaiagent.agent.todo.TodoItem;
import com.kiwi.keweiaiagent.agent.todo.TodoSnapshot;
import com.kiwi.keweiaiagent.exception.BusinessException;
import com.kiwi.keweiaiagent.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.AskUserQuestionTool;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BaseAgentTest {

    @Test
    void runShouldThrowWhenAgentIsNotIdle() {
        BaseAgent agent = new TestAgent();
        agent.setState(AgentState.RUNNING);

        BusinessException ex = assertThrows(BusinessException.class, () -> agent.run("hello"));

        assertEquals(ErrorCode.AGENT_BUSY.getCode(), ex.getCode());
    }

    @Test
    void runShouldThrowWhenPromptIsBlank() {
        BaseAgent agent = new TestAgent();

        BusinessException ex = assertThrows(BusinessException.class, () -> agent.run("   "));

        assertEquals(ErrorCode.INVALID_PARAM.getCode(), ex.getCode());
    }

    @Test
    void runShouldReturnStepResultWhenSuccess() {
        BaseAgent agent = new TestAgent();

        String result = agent.run("hello");

        assertEquals("Step 1 result: ok", result);
        assertEquals(AgentState.FINISHED, agent.getState());
    }

    @Test
    void runStreamShouldThrowWhenAgentIsNotIdle() {
        BaseAgent agent = new TestAgent();
        agent.setState(AgentState.RUNNING);

        BusinessException ex = assertThrows(BusinessException.class, () -> agent.runStream("hello"));

        assertEquals(ErrorCode.AGENT_BUSY.getCode(), ex.getCode());
    }

    @Test
    void runStreamShouldThrowWhenPromptIsBlank() {
        BaseAgent agent = new TestAgent();

        BusinessException ex = assertThrows(BusinessException.class, () -> agent.runStream("   "));

        assertEquals(ErrorCode.INVALID_PARAM.getCode(), ex.getCode());
    }

    @Test
    void runStreamShouldCompleteWhenSuccess() throws InterruptedException {
        BaseAgent agent = new TestAgent();
        agent.runStream("hello");

        assertTrue(waitForState(agent, AgentState.FINISHED));
    }

    @Test
    void runStreamShouldSetErrorStateWhenStepThrowsBusinessException() throws InterruptedException {
        BaseAgent agent = new ErrorAgent();
        agent.runStream("hello");

        assertTrue(waitForState(agent, AgentState.ERROR));
    }

    @Test
    void runStreamShouldWaitForUserInputWhenQuestionIsRaised() throws InterruptedException {
        BaseAgent agent = new WaitingAgent();
        agent.runStream("hello");

        assertTrue(waitForState(agent, AgentState.WAITING_FOR_USER_INPUT));
    }

    @Test
    void shouldBuildTodoEventPayloadFromSessionStore() {
        ManusSessionStore store = new ManusSessionStore();
        store.putSession("chat-1", "hello", null);
        store.saveTodoSnapshot("chat-1", new TodoSnapshot(List.of(
                new TodoItem("plan", "拆解任务", "completed"),
                new TodoItem("build", "执行任务", "in_progress")
        )));
        BaseAgent agent = new TestAgent();
        agent.setManusSessionStore(store);
        agent.setSessionId("chat-1");

        BaseAgent.TodoEventPayload payload = agent.buildTodoEventPayload();

        assertEquals(2, payload.todo().items().size());
        assertEquals("build", payload.todo().items().get(1).id());
    }

    private static class TestAgent extends BaseAgent {
        @Override
        public String step() {
            setState(AgentState.FINISHED);
            return "ok";
        }
    }

    private static class ErrorAgent extends BaseAgent {
        @Override
        public String step() {
            throw new BusinessException(ErrorCode.AGENT_RUN_FAILED, "step failed");
        }
    }

    private static class WaitingAgent extends BaseAgent {
        @Override
        public String step() {
            throw new PendingUserQuestionException(List.of(
                    new AskUserQuestionTool.Question("question", "header", List.of(
                            new AskUserQuestionTool.Question.Option("A", "A"),
                            new AskUserQuestionTool.Question.Option("B", "B")
                    ), false)
            ));
        }
    }

    private boolean waitForState(BaseAgent agent, AgentState targetState) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            if (agent.getState() == targetState) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }
}
