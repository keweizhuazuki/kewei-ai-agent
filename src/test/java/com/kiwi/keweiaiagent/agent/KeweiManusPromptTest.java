package com.kiwi.keweiaiagent.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class KeweiManusPromptTest {

    @Test
    void shouldMentionTodoPlanningForComplexTasks() {
        KeweiManus manus = new KeweiManus(new ToolCallback[0], mock(ChatModel.class));

        assertTrue(manus.getSystemPrompt().contains("TodoWrite"));
        assertTrue(manus.getSystemPrompt().contains("multi-step"));
    }
}
