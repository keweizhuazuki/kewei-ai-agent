package com.kiwi.keweiaiagent.tools;

import com.kiwi.keweiaiagent.agent.ManusSessionStore;
import com.kiwi.keweiaiagent.agent.todo.CommunityTodoMapper;
import com.kiwi.keweiaiagent.agent.todo.TodoSnapshot;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TodoWriteToolTest {

    @Test
    void shouldAcceptStructuredJsonArgumentsAndSaveTodoSnapshot() {
        ManusSessionStore store = new ManusSessionStore();
        store.putSession("chat-1", "帮我做个方案", null);
        store.activateSession("chat-1");
        TodoWriteTool delegate = TodoWriteTool.builder()
                .todoEventHandler(todos -> store.saveTodoSnapshot("chat-1", CommunityTodoMapper.toSnapshot(todos)))
                .build();
        ToolCallback callback = ToolCallbacks.from(new TodoWriteToolAdapter(delegate))[0];

        String result = callback.call("""
                {"todos":[
                  {"content":"拆解需求","status":"completed","activeForm":"拆解需求中"},
                  {"content":"开始实现","status":"in_progress","activeForm":"开始实现中"}
                ]}
                """);

        TodoSnapshot snapshot = store.getTodoSnapshot("chat-1");
        assertTrue(result.contains("Todos have been modified successfully"));
        assertEquals(2, snapshot.items().size());
        assertEquals("开始实现", snapshot.items().get(1).content());
    }

    @Test
    void shouldRejectMultipleInProgressItems() {
        ManusSessionStore store = new ManusSessionStore();
        store.putSession("chat-1", "帮我做个方案", null);
        store.activateSession("chat-1");
        TodoWriteTool delegate = TodoWriteTool.builder()
                .todoEventHandler(todos -> store.saveTodoSnapshot("chat-1", CommunityTodoMapper.toSnapshot(todos)))
                .build();
        ToolCallback callback = ToolCallbacks.from(new TodoWriteToolAdapter(delegate))[0];

        String result = callback.call("""
                {"todos":[
                  {"content":"拆解需求","status":"in_progress","activeForm":"拆解需求中"},
                  {"content":"开始实现","status":"in_progress","activeForm":"开始实现中"}
                ]}
                """);

        assertTrue(result.contains("Only ONE task can be in_progress"));
        assertNull(store.getTodoSnapshot("chat-1"));
    }
}
