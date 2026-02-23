package com.kiwi.keweiaiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("ollama")
public class OllamaAiInvoke implements CommandLineRunner {

    @Resource(name = "ollamaChatModel")
    private ChatModel ollamaChatModel;

    @Override
    public void run(String... args) throws Exception {
        AssistantMessage msg = ollamaChatModel.call(new Prompt("你好我是kiw"))
                .getResult()
                .getOutput();
        System.out.println(msg.getText());
    }
}
