package com.kiwi.keweiaiagent.tooltape;

import com.kiwi.keweiaiagent.agent.ToolCallAgent;
import dev.tooltape.CapturePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.kiwi.keweiaiagent.tooltape.ToolTapeSessionTest.callback;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the application's actual ToolCallingManager dispatch without a model service or network. */
class ToolTapeAgentReplayTest {
    @TempDir Path directory;

    @Test
    void syntheticModelToolPlanRecordsAndReplaysThroughActualAgentAct() throws Exception {
        Path tape = directory.resolve("agent.tape");
        CapturePolicy policy = CapturePolicy.allow(Set.of("city"), Set.of(ToolTapeSession.RESULT_FIELD));
        var calls = new AtomicInteger();
        ToolCallback live = callback("forecast", calls, "Synthetic sunshine");
        String recorded;
        try (ToolTapeSession session = ToolTapeSession.record(tape, policy)) {
            ToolCallAgent agent = agent(session.wrap(live));
            recorded = agent.act();
            assertTrue(recorded.contains("Synthetic sunshine"));
            assertInstanceOf(ToolResponseMessage.class, agent.getMessageList().getLast());
        }
        assertEquals(1, calls.get());
        try (ToolTapeSession session = ToolTapeSession.replay(tape, policy)) {
            ToolCallAgent agent = agent(session.wrap(live));
            assertEquals(recorded, agent.act());
            ToolResponseMessage response = (ToolResponseMessage) agent.getMessageList().getLast();
            assertEquals("Synthetic sunshine", response.getResponses().getFirst().responseData());
        }
        assertEquals(1, calls.get(), "Replay must not invoke the live tool a second time");
    }

    private ToolCallAgent agent(ToolCallback tool) {
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[] { tool });
        agent.setSystemPrompt("Synthetic offline tool plan");
        agent.getMessageList().add(new UserMessage("Synthetic offline task"));
        AssistantMessage plan = AssistantMessage.builder().content("").toolCalls(List.of(
                new AssistantMessage.ToolCall("fixture-call", "function", "forecast", "{\"city\":\"Paris\"}")))
                .build();
        // The model is a deterministic local stub, not a recorded/replayed LLM response.
        agent.setChatClient(ChatClient.builder(new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(plan)));
            }
        }).build());
        assertTrue(agent.think());
        return agent;
    }
}
