package com.kiwi.keweiaiagent.chatmemory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiwi.keweiaiagent.chatmemory.entity.ChatMemoryMessageDO;
import com.kiwi.keweiaiagent.chatmemory.mapper.ChatMemoryMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 MySQL 的聊天记忆实现，负责消息的数据库持久化。
 */
@RequiredArgsConstructor
public class MySqlChatMemory implements ChatMemory {

    /**
     * JSON 序列化工具。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    /**
     * 聊天记忆消息数据访问接口。
     */
    private final ChatMemoryMessageMapper chatMemoryMessageMapper;

    /**
     * 向数据库中追加指定会话的消息记录。
     */
    @Override
    public void add(String conversationId, Message message) {
        add(conversationId, List.of(message));
    }

    /**
     * 向数据库中追加指定会话的消息记录。
     */
    @Override
    public synchronized void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        for (Message message : messages) {
            ChatMemoryMessageDO record = new ChatMemoryMessageDO();
            record.setConversationId(conversationId);
            record.setPayloadJson(serializeMessage(message));
            chatMemoryMessageMapper.insert(record);
        }
    }

    /**
     * 从数据库中读取指定会话的历史消息。
     */
    @Override
    public synchronized List<Message> get(String conversationId) {
        LambdaQueryWrapper<ChatMemoryMessageDO> queryWrapper = new LambdaQueryWrapper<ChatMemoryMessageDO>()
                .eq(ChatMemoryMessageDO::getConversationId, conversationId)
                .orderByAsc(ChatMemoryMessageDO::getId);
        return chatMemoryMessageMapper.selectList(queryWrapper).stream()
                .map(ChatMemoryMessageDO::getPayloadJson)
                .map(this::deserializeMessage)
                .toList();
    }

    /**
     * 删除指定会话在数据库中的历史消息。
     */
    @Override
    public synchronized void clear(String conversationId) {
        LambdaQueryWrapper<ChatMemoryMessageDO> queryWrapper = new LambdaQueryWrapper<ChatMemoryMessageDO>()
                .eq(ChatMemoryMessageDO::getConversationId, conversationId);
        chatMemoryMessageMapper.delete(queryWrapper);
    }

    /**
     * 将消息对象序列化为字符串。
     */
    private String serializeMessage(Message message) {
        try {
            return OBJECT_MAPPER.writeValueAsString(fromSpringMessage(message));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize chat memory message", e);
        }
    }

    /**
     * 将字符串反序列化为消息对象。
     */
    private Message deserializeMessage(String payloadJson) {
        try {
            StoredMessage storedMessage = OBJECT_MAPPER.readValue(payloadJson, StoredMessage.class);
            return toSpringMessage(storedMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize chat memory message", e);
        }
    }

    /**
     * 将 Spring AI 消息对象转换为数据库可存储结构。
     */
    private StoredMessage fromSpringMessage(Message message) {
        StoredMessage stored = new StoredMessage();
        stored.type = message.getMessageType().name();
        stored.text = message.getText();
        Map<String, Object> metadata = message.getMetadata();
        stored.metadata = metadata == null ? Map.of() : new HashMap<>(metadata);

        if (message instanceof AssistantMessage assistantMessage) {
            stored.toolCalls = assistantMessage.getToolCalls().stream()
                    .map(tc -> new StoredToolCall(tc.id(), tc.type(), tc.name(), tc.arguments()))
                    .toList();
        }

        if (message instanceof ToolResponseMessage toolResponseMessage) {
            stored.toolResponses = toolResponseMessage.getResponses().stream()
                    .map(tr -> new StoredToolResponse(tr.id(), tr.name(), tr.responseData()))
                    .toList();
        }
        return stored;
    }

    /**
     * 将数据库存储结构恢复为 Spring AI 消息对象。
     */
    private Message toSpringMessage(StoredMessage stored) {
        MessageType type = MessageType.valueOf(stored.type);
        Map<String, Object> metadata = stored.metadata == null ? Collections.emptyMap() : stored.metadata;
        String text = stored.text == null ? "" : stored.text;

        return switch (type) {
            case USER -> UserMessage.builder()
                    .text(text)
                    .metadata(metadata)
                    .build();
            case SYSTEM -> SystemMessage.builder()
                    .text(text)
                    .metadata(metadata)
                    .build();
            case ASSISTANT -> AssistantMessage.builder()
                    .content(text)
                    .properties(metadata)
                    .toolCalls(stored.toolCalls == null ? List.of() : stored.toolCalls.stream()
                            .map(tc -> new AssistantMessage.ToolCall(tc.id, tc.type, tc.name, tc.arguments))
                            .toList())
                    .build();
            case TOOL -> ToolResponseMessage.builder()
                    .metadata(metadata)
                    .responses(stored.toolResponses == null ? List.of() : stored.toolResponses.stream()
                            .map(tr -> new ToolResponseMessage.ToolResponse(tr.id, tr.name, tr.responseData))
                            .toList())
                    .build();
        };
    }

    /**
     * 持久化消息对象，封装基础消息的序列化结果。
     */
    private static class StoredMessage {
        public String type;
        public String text;
        public Map<String, Object> metadata = Map.of();
        public List<StoredToolCall> toolCalls = List.of();
        public List<StoredToolResponse> toolResponses = List.of();
    }

    /**
     * 持久化工具调用对象，封装工具调用参数的序列化结果。
     */
    private static class StoredToolCall {
        public String id;
        public String type;
        public String name;
        public String arguments;

        public StoredToolCall() {
        }

        public StoredToolCall(String id, String type, String name, String arguments) {
            this.id = id;
            this.type = type;
            this.name = name;
            this.arguments = arguments;
        }
    }

    /**
     * 持久化工具响应对象，封装工具执行结果的序列化内容。
     */
    private static class StoredToolResponse {
        public String id;
        public String name;
        public String responseData;

        public StoredToolResponse() {
        }

        public StoredToolResponse(String id, String name, String responseData) {
            this.id = id;
            this.name = name;
            this.responseData = responseData;
        }
    }
}
