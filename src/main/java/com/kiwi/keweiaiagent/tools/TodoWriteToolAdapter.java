package com.kiwi.keweiaiagent.tools;

import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TodoWriteToolAdapter {

    private final TodoWriteTool delegate;

    public TodoWriteToolAdapter(TodoWriteTool delegate) {
        this.delegate = delegate;
    }

    @Tool(name = "TodoWrite", description = """
            Use this tool to create and manage a structured task list for the current task.
            Provide a todos array where each item includes content, status, and activeForm.
            Use it for complex multi-step work before executing other tools.
            """, returnDirect = false)
    public String todoWrite(
            @ToolParam(description = "Structured todo items for the current task") List<TodoWriteTool.Todos.TodoItem> todos
    ) {
        try {
            return delegate.todoWrite(new TodoWriteTool.Todos(todos));
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}
