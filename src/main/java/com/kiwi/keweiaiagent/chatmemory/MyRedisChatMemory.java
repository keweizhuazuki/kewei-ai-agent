package com.kiwi.keweiaiagent.chatmemory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyRedisChatMemory implements ChatMemory {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private final StringRedisTemplate stringRedisTemplate;
    private final String keyPrefix;

    public MyRedisChatMemory(StringRedisTemplate stringRedisTemplate, String keyPrefix) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.keyPrefix = (keyPrefix == null || keyPrefix.isBlank()) ? "chat:memory:" : keyPrefix;
    }

    @Override
    public void add(String conversationId, Message message) {
        add(conversationId, List.of(message));
    }

    @Override
    public synchronized void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        List<String> payloads = messages.stream()
                .map(this::serializeMessage)
                .toList();
        stringRedisTemplate.opsForList().rightPushAll(buildKey(conversationId), payloads);
    }

    @Override
    public synchronized List<Message> get(String conversationId) {
        List<String> payloads = stringRedisTemplate.opsForList().range(buildKey(conversationId), 0, -1);
        if (payloads == null || payloads.isEmpty()) {
            return List.of();
        }
        return payloads.stream()
                .map(this::deserializeMessage)
                .toList();
    }

    @Override
    public synchronized void clear(String conversationId) {
        stringRedisTemplate.delete(buildKey(conversationId));
    }

    private String buildKey(String conversationId) {
        return keyPrefix + conversationId;
    }

    private String serializeMessage(Message message) {
        try {
            return OBJECT_MAPPER.writeValueAsString(fromSpringMessage(message));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize chat memory message", e);
        }
    }

    private Message deserializeMessage(String payloadJson) {
        try {
            StoredMessage storedMessage = OBJECT_MAPPER.readValue(payloadJson, StoredMessage.class);
            return toSpringMessage(storedMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize chat memory message", e);
        }
    }

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

    private static class StoredMessage {
        public String type;
        public String text;
        public Map<String, Object> metadata = Map.of();
        public List<StoredToolCall> toolCalls = List.of();
        public List<StoredToolResponse> toolResponses = List.of();
    }

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
