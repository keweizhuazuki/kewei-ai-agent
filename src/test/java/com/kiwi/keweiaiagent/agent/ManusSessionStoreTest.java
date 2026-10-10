package com.kiwi.keweiaiagent.agent;

import com.kiwi.keweiaiagent.agent.todo.TodoItem;
import com.kiwi.keweiaiagent.agent.todo.TodoSnapshot;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManusSessionStoreTest {

    @Test
    void shouldConsumePendingAnswersOnlyOnceAcrossConcurrentCallers() throws Exception {
        InterleavingSessionMap sessions = new InterleavingSessionMap();
        ManusSessionStore store = storeWithSessions(sessions);
        store.putSession("chat-1", "task", null);
        store.submitAnswers("chat-1", Map.of("q_1", "A"));
        CyclicBarrier bothRead = new CyclicBarrier(2);
        sessions.afterGet = () -> {
            try {
                bothRead.await(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        };
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> store.consumeAnswers("chat-1"));
            var second = executor.submit(() -> store.consumeAnswers("chat-1"));
            Map<String, String> firstResult = first.get(5, TimeUnit.SECONDS);
            Map<String, String> secondResult = second.get(5, TimeUnit.SECONDS);

            assertEquals(1, (firstResult == null ? 0 : 1) + (secondResult == null ? 0 : 1));
            assertEquals(Map.of("q_1", "A"), firstResult == null ? secondResult : firstResult);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void consumingAnswersShouldNotOverwriteAnInterveningTodoUpdate() {
        InterleavingSessionMap sessions = new InterleavingSessionMap();
        ManusSessionStore store = storeWithSessions(sessions);
        store.putSession("chat-1", "task", null);
        store.submitAnswers("chat-1", Map.of("q_1", "A"));
        TodoSnapshot updated = new TodoSnapshot(List.of(new TodoItem("plan", "更新计划", "completed")));
        sessions.interleaveOnce(() -> store.saveTodoSnapshot("chat-1", updated));

        assertEquals(Map.of("q_1", "A"), store.consumeAnswers("chat-1"));
        assertEquals(updated, store.getTodoSnapshot("chat-1"));
        assertNull(store.consumeAnswers("chat-1"));
    }

    @Test
    void consumingAnswersShouldNotResurrectAnInterveninglyRemovedSession() {
        InterleavingSessionMap sessions = new InterleavingSessionMap();
        ManusSessionStore store = storeWithSessions(sessions);
        store.putSession("chat-1", "task", null);
        store.submitAnswers("chat-1", Map.of("q_1", "A"));
        sessions.interleaveOnce(() -> store.removeSession("chat-1"));

        store.consumeAnswers("chat-1");

        assertNull(store.getSession("chat-1"));
    }

    @Test
    void shouldMapQuestionIdsAndLeaveNoAnswersAfterConsumption() {
        ManusSessionStore store = new ManusSessionStore();
        store.putSession("chat-1", "task", null);
        store.savePendingQuestions("chat-1", List.of(question("选择格式？", options())));
        store.submitAnswers("chat-1", Map.of("q_1", "PDF", "unknown", "ignored"));

        assertEquals(Map.of("选择格式？", "PDF"), store.consumeAnswers("chat-1"));
        assertNull(store.consumeAnswers("chat-1"));
        assertNull(store.consumeAnswers("missing"));
    }

    @Test
    void shouldSnapshotSubmittedAnswersAndPreventExternalMutation() {
        ManusSessionStore store = new ManusSessionStore();
        store.putSession("chat-1", "task", null);
        Map<String, String> answers = new HashMap<>(Map.of("q_1", "original"));
        store.submitAnswers("chat-1", answers);
        answers.put("q_1", "changed");

        assertEquals("original", store.getSession("chat-1").pendingAnswers().get("q_1"));
        assertThrows(UnsupportedOperationException.class,
                () -> store.getSession("chat-1").pendingAnswers().put("q_1", "changed again"));
        assertEquals(Map.of("q_1", "original"), store.consumeAnswers("chat-1"));
    }

    @Test
    void shouldSnapshotQuestionListsAndNestedOptions() {
        ManusSessionStore store = new ManusSessionStore();
        store.putSession("chat-1", "task", null);
        var options = new ArrayList<>(options());
        var questions = new ArrayList<>(List.of(question("选择格式？", options)));
        store.savePendingQuestions("chat-1", questions);
        options.clear();
        questions.clear();

        ManusSessionStore.ManusSession session = store.getSession("chat-1");
        assertEquals(1, session.rawPendingQuestions().size());
        assertEquals("A", session.rawPendingQuestions().getFirst().options().getFirst().label());
        assertThrows(UnsupportedOperationException.class, () -> session.rawPendingQuestions().clear());
        assertThrows(UnsupportedOperationException.class, () -> session.rawPendingQuestions().getFirst().options().clear());
        assertThrows(UnsupportedOperationException.class, () -> store.getPendingQuestions("chat-1").clear());
        assertThrows(UnsupportedOperationException.class, () -> session.pendingQuestions().getFirst().options().clear());
        store.submitAnswers("chat-1", Map.of("q_1", "A"));
        assertEquals(Map.of("选择格式？", "A"), store.consumeAnswers("chat-1"));
    }

    @Test
    void shouldSnapshotTodoItemsBeforeStoringAndBroadcasting() {
        ManusSessionStore store = new ManusSessionStore();
        store.putSession("chat-1", "task", null);
        var items = new ArrayList<>(List.of(new TodoItem("plan", "列出方案", "pending")));
        AtomicReference<TodoSnapshot> received = new AtomicReference<>();
        store.registerTodoSnapshotListener("chat-1", received::set);
        store.saveTodoSnapshot("chat-1", new TodoSnapshot(items));
        items.clear();

        assertEquals(1, store.getTodoSnapshot("chat-1").items().size());
        assertEquals(1, received.get().items().size());
        assertThrows(UnsupportedOperationException.class, () -> received.get().items().clear());
        assertThrows(UnsupportedOperationException.class, () -> store.getTodoSnapshot("chat-1").items().clear());
    }

    @Test
    void shouldKeepNullAnswersCompatible() {
        ManusSessionStore store = new ManusSessionStore();
        store.putSession("chat-1", "task", null);
        store.savePendingQuestions("chat-1", List.of(question("自由回答？", options())));
        Map<String, String> answers = new HashMap<>();
        answers.put("q_1", null);
        store.submitAnswers("chat-1", answers);
        assertEquals(Map.of(), store.consumeAnswers("chat-1"));
        store.submitAnswers("chat-1", null);
        assertNull(store.consumeAnswers("chat-1"));
    }

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

    private static AskUserQuestionTool.Question question(String text, List<AskUserQuestionTool.Question.Option> options) {
        return new AskUserQuestionTool.Question(text, "格式", options, false);
    }

    private static List<AskUserQuestionTool.Question.Option> options() {
        return List.of(new AskUserQuestionTool.Question.Option("A", "first"),
                new AskUserQuestionTool.Question.Option("B", "second"));
    }

    private static ManusSessionStore storeWithSessions(InterleavingSessionMap sessions) {
        ManusSessionStore store = new ManusSessionStore();
        ReflectionTestUtils.setField(store, "sessions", sessions);
        return store;
    }

    /**
     * 在旧实现的读取与写回之间精确插入更新，无需 sleep 或概率性并发循环。
     * 原子实现会先执行同一更新，再在 computeIfPresent 中读取最新会话。
     */
    private static class InterleavingSessionMap extends ConcurrentHashMap<String, ManusSessionStore.ManusSession> {
        private Runnable afterGet;
        private Runnable beforeCompute;

        void interleaveOnce(Runnable update) {
            AtomicBoolean invoked = new AtomicBoolean();
            Runnable once = () -> {
                if (invoked.compareAndSet(false, true)) update.run();
            };
            afterGet = once;
            beforeCompute = once;
        }

        @Override
        public ManusSessionStore.ManusSession get(Object key) {
            ManusSessionStore.ManusSession session = super.get(key);
            if (afterGet != null) afterGet.run();
            return session;
        }

        @Override
        public ManusSessionStore.ManusSession computeIfPresent(String key,
                BiFunction<? super String, ? super ManusSessionStore.ManusSession, ? extends ManusSessionStore.ManusSession> function) {
            if (beforeCompute != null) beforeCompute.run();
            return super.computeIfPresent(key, function);
        }
    }
}
