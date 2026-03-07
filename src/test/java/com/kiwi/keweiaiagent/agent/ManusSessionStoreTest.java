package com.kiwi.keweiaiagent.agent;

import com.kiwi.keweiaiagent.agent.todo.TodoItem;
import com.kiwi.keweiaiagent.agent.todo.TodoSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ManusSessionStoreTest {

    @Test
    void shouldSaveAndReadTodoSnapshotForSession() {
        ManusSessionStore store = new ManusSessionStore();
        store.putSession("chat-1", "帮我写一个计划", null);
        TodoSnapshot snapshot = new TodoSnapshot(List.of(
                new TodoItem("plan", "列出方案", "completed"),
                new TodoItem("build", "开始实现", "in_progress")
        ));

        store.saveTodoSnapshot("chat-1", snapshot);

        assertEquals(snapshot, store.getTodoSnapshot("chat-1"));
    }

    @Test
    void shouldClearTodoSnapshotWhenSessionRemoved() {
        ManusSessionStore store = new ManusSessionStore();
        store.putSession("chat-1", "帮我写一个计划", null);
        store.saveTodoSnapshot("chat-1", new TodoSnapshot(List.of(
                new TodoItem("plan", "列出方案", "completed")
        )));

        store.removeSession("chat-1");

        assertNull(store.getTodoSnapshot("chat-1"));
    }

    @Test
    void shouldNotifyTodoSnapshotListenersImmediately() {
        ManusSessionStore store = new ManusSessionStore();
        store.putSession("chat-1", "帮我写一个计划", null);
        TodoSnapshot snapshot = new TodoSnapshot(List.of(
                new TodoItem("plan", "列出方案", "in_progress")
        ));
        AtomicReference<TodoSnapshot> received = new AtomicReference<>();
        ManusSessionStore.TodoSnapshotListener listener = received::set;
        store.registerTodoSnapshotListener("chat-1", listener);

        store.saveTodoSnapshot("chat-1", snapshot);

        assertEquals(snapshot, received.get());
    }

    @Test
    void shouldRemoveTodoSnapshotListenersWithSession() {
        ManusSessionStore store = new ManusSessionStore();
        store.putSession("chat-1", "帮我写一个计划", null);
        AtomicReference<TodoSnapshot> received = new AtomicReference<>();
        store.registerTodoSnapshotListener("chat-1", received::set);

        store.removeSession("chat-1");
        store.saveTodoSnapshot("chat-1", new TodoSnapshot(List.of(
                new TodoItem("plan", "列出方案", "completed")
        )));

        assertNull(received.get());
    }
}
