package com.kiwi.keweiimagegenerationmcpserver;

import com.kiwi.keweiimagegenerationmcpserver.tools.ImageGenerationTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class KeweiImageGenerationMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeweiImageGenerationMcpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider imageGenerationTools(ImageGenerationTool imageGenerationTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(imageGenerationTool)
                .build();
    }
}
