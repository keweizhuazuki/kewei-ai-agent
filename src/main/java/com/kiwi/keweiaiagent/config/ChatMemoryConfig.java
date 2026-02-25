package com.kiwi.keweiaiagent.config;

import com.kiwi.keweiaiagent.chatmemory.FileBaseChatMemory;
import com.kiwi.keweiaiagent.chatmemory.MyRedisChatMemory;
import com.kiwi.keweiaiagent.chatmemory.MySqlChatMemory;
import com.kiwi.keweiaiagent.chatmemory.mapper.ChatMemoryMessageMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class ChatMemoryConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.chat-memory", name = "type", havingValue = "mysql")
    public ChatMemory mysqlChatMemory(ChatMemoryMessageMapper chatMemoryMessageMapper) {
        return new MySqlChatMemory(chatMemoryMessageMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.chat-memory", name = "type", havingValue = "redis")
    public ChatMemory redisChatMemory(
            StringRedisTemplate stringRedisTemplate,
            @Value("${app.chat-memory.redis.key-prefix:chat:memory:}") String keyPrefix
    ) {
        return new MyRedisChatMemory(stringRedisTemplate, keyPrefix);
    }

    @Bean
    @ConditionalOnMissingBean(ChatMemory.class)
    public ChatMemory fileChatMemory(
            @Value("${app.chat-memory.file-dir:${user.dir}/tmp/chat-memory}") String fileDir
    ) {
        return new FileBaseChatMemory(fileDir);
    }
}
