package com.kiwi.keweiaiagent.tools;


import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class TerminateTool {

    @Tool(description = """
            Terminate the interaction when the request is met or if the assistant cannot proceed further with the task.
            When you have finished tall the tasks, call this tool to end the work.
            """)
    public String doTerminate(){
        return "任务结束";
    }
}
