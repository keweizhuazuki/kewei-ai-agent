package com.kiwi.keweiaiagent.agent.todo;

import org.springaicommunity.agent.tools.TodoWriteTool;

import java.util.ArrayList;
import java.util.List;

public final class CommunityTodoMapper {

    private CommunityTodoMapper() {
    }

    public static TodoSnapshot toSnapshot(TodoWriteTool.Todos todos) {
        List<TodoItem> items = new ArrayList<>();
        List<TodoWriteTool.Todos.TodoItem> communityItems = todos == null ? List.of() : todos.todos();
        for (int i = 0; i < communityItems.size(); i++) {
            TodoWriteTool.Todos.TodoItem item = communityItems.get(i);
            items.add(new TodoItem(
                    "todo_" + (i + 1),
                    item.content(),
                    item.status().name()
            ));
        }
        return new TodoSnapshot(items);
    }
}
