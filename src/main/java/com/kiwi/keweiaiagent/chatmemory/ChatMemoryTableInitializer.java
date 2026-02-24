package com.kiwi.keweiaiagent.chatmemory;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.chat-memory", name = "type", havingValue = "mysql")
@RequiredArgsConstructor
public class ChatMemoryTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_chat_memory_message (
                    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                    conversation_id VARCHAR(128) NOT NULL COMMENT '会话ID',
                    payload_json LONGTEXT NOT NULL COMMENT '消息JSON',
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    PRIMARY KEY (id),
                    KEY idx_conversation_id (conversation_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天记忆消息表'
                """);
    }
}
