package com.kiwi.keweiaiagent.chatmemory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileBaseChatMemory implements ChatMemory {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final TypeReference<List<StoredMessage>> STORED_MESSAGE_LIST_TYPE = new TypeReference<>() {};

    private final String Base_DIR;

    public FileBaseChatMemory(String base_DIR) {
        this.Base_DIR = base_DIR;
        File file = new File(base_DIR);
        if (!file.exists()) {
            boolean mkdirs = file.mkdirs();
            if (!mkdirs) {
                throw new RuntimeException("Failed to create directory: " + base_DIR);
            }
        }
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
        List<Message> allMessages = getOrCreateConversation(conversationId);
        allMessages.addAll(messages);
        saveConversation(conversationId, allMessages);
    }

    @Override
    public synchronized List<Message> get(String conversationId) {
        return List.copyOf(getOrCreateConversation(conversationId));
    }

    @Override
    public synchronized void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        if (!file.exists()) {
            return;
        }
        if (!file.delete()) {
            throw new RuntimeException("Failed to delete conversation file: " + file.getAbsolutePath());
        }
    }

    /**
     * 获取或创建会话文件，返回消息列表
     * @param conversationId
     * @return
     */
    private List<Message> getOrCreateConversation(String conversationId){
        File conversationFile = getConversationFile(conversationId);
        if (conversationFile.exists()) {
            try {
                if (conversationFile.length() == 0L) {
                    return new java.util.ArrayList<>();
                }
                List<StoredMessage> storedMessages = OBJECT_MAPPER.readValue(conversationFile, STORED_MESSAGE_LIST_TYPE);
                return storedMessages == null ? new java.util.ArrayList<>() : storedMessages.stream()
                        .map(this::toSpringMessage)
                        .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
            } catch (Exception e) {
                throw new RuntimeException("Error reading conversation file: " + conversationFile.getAbsolutePath(), e);
            }
        } else {
            // 文件不存在，创建一个新的空文件
            try {
                boolean created = conversationFile.createNewFile();
                if (!created) {
                    throw new RuntimeException("Failed to create conversation file: " + conversationFile.getAbsolutePath());
                }
                OBJECT_MAPPER.writeValue(conversationFile, List.of());
            } catch (Exception e) {
                throw new RuntimeException("Error creating conversation file: " + e.getMessage(), e);
            }
        }
        return new java.util.ArrayList<>();
    }

    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            List<StoredMessage> payload = messages.stream()
                    .map(this::fromSpringMessage)
                    .toList();
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(tempFile, payload);
            try {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error saving conversation file: " + file.getAbsolutePath(), e);
        } finally {
            if (tempFile.exists() && !tempFile.equals(file)) {
                // 清理异常场景遗留的临时文件
                tempFile.delete();
            }
        }

    }

    private File getConversationFile(String conversationId) {
        String safeFileName = URLEncoder.encode(conversationId, StandardCharsets.UTF_8) + ".json";
        return new File(Base_DIR, safeFileName);
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
