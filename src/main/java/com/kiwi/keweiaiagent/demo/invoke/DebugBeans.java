package com.kiwi.keweiaiagent.demo.invoke;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class DebugBeans implements CommandLineRunner {
    private final ApplicationContext ctx;
    public DebugBeans(ApplicationContext ctx) { this.ctx = ctx; }

    @Override
    public void run(String... args) {
        String[] names = ctx.getBeanNamesForType(org.springframework.ai.chat.model.ChatModel.class);
        System.out.println("ChatModel beans = " + Arrays.toString(names));
    }
}

