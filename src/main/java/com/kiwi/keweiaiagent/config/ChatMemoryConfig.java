package com.kiwi.keweiaiagent.config;

import com.kiwi.keweiaiagent.chatmemory.FileBaseChatMemory;
import com.kiwi.keweiaiagent.chatmemory.MySqlChatMemory;
import com.kiwi.keweiaiagent.chatmemory.mapper.ChatMemoryMessageMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemory chatMemory(
            @Value("${app.chat-memory.type:file}") String type,
            @Value("${app.chat-memory.file-dir:${user.dir}/tmp/chat-memory}") String fileDir,
            ObjectProvider<ChatMemoryMessageMapper> mapperProvider
    ) {
        if ("mysql".equalsIgnoreCase(type)) {
            ChatMemoryMessageMapper mapper = mapperProvider.getIfAvailable();
            if (mapper == null) {
                throw new IllegalStateException("app.chat-memory.type=mysql but ChatMemoryMessageMapper is unavailable");
            }
            return new MySqlChatMemory(mapper);
        }
        return new FileBaseChatMemory(fileDir);
    }
}
