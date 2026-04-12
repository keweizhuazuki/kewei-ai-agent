package com.kiwi.keweiaiagent.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KeweiManusPromptTest {

    @Test
    void shouldMentionTodoPlanningForComplexTasks() {
        KeweiManus manus = new KeweiManus(new ToolCallback[0], new NoOpChatModel());

        assertTrue(manus.getSystemPrompt().contains("TodoWrite"));
        assertTrue(manus.getSystemPrompt().contains("multi-step"));
    }

    @Test
    void shouldTreatOpenClawDelegationAsAtomicTodoStep() {
        KeweiManus manus = new KeweiManus(new ToolCallback[0], new NoOpChatModel());

        assertTrue(manus.getSystemPrompt().contains("delegateResearchToOpenClaw"));
        assertTrue(manus.getSystemPrompt().contains("atomic todo step"));
        assertTrue(manus.getNextStepPrompt().contains("orchestration level only"));
    }

    @Test
    void shouldIncludeLongTermMemoryPromptWhenProvided() {
        KeweiManus manus = new KeweiManus(
                new ToolCallback[0],
                new NoOpChatModel(),
                "Use MEMORY.md and MemoryView for durable facts."
        );

        assertTrue(manus.getSystemPrompt().contains("MEMORY.md"));
        assertTrue(manus.getSystemPrompt().contains("durable facts"));
    }

    private static final class NoOpChatModel implements ChatModel {
        @Override
        public ChatResponse call(Prompt prompt) {
            return null;
        }
    }
}
