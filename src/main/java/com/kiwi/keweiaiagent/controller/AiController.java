package com.kiwi.keweiaiagent.controller;

import com.kiwi.keweiaiagent.agent.KeweiManus;
import com.kiwi.keweiaiagent.agent.ManusSessionService;
import com.kiwi.keweiaiagent.app.LoveApp;
import com.kiwi.keweiaiagent.app.TodoDemoApp;
import com.kiwi.keweiaiagent.constant.FileConstant;
import com.kiwi.keweiaiagent.exception.BusinessException;
import com.kiwi.keweiaiagent.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 控制器，负责暴露聊天、Manus 会话和文件上传等接口。
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    /**
     * 默认 SSE 连接超时时间。
     */
    private static final long DEFAULT_SSE_TIMEOUT = 0L;

    /**
     * 恋爱助手应用服务。
     */
    @Resource
    private LoveApp loveApp;

    /**
     * 当前系统注册的全部工具。
     */
    @Resource
    private ToolCallback[] allTools;

    /**
     * 对话使用的大模型实例。
     */
    @Resource
    private ChatModel ollamaChatModel;

    /**
     * JSON 序列化工具。
     */
    @Resource
    private ObjectMapper objectMapper;

    /**
     * Manus 会话服务。
     */
    @Resource
    private ManusSessionService manusSessionService;

    /**
     * Todo 演示应用服务。
     */
    @Resource
    private TodoDemoApp todoDemoApp;

    /**
     * 同步调用恋爱助手完成一次对话。
     */
    @GetMapping("/love_app/chat/sync")
    public Object doChatWithLoveAppSync(String message, String chatId, String option, String imagePath){
        if (shouldUseTodoDemoOption(option)) {
            return todoDemoApp.call(message, chatId);
        }
        if (shouldUseSkillsOption(option)) {
            return loveApp.callWithSkills(message, chatId);
        }
        if (shouldUseImageOption(option, imagePath)) {
            return loveApp.doChatWithImage(message, chatId, imagePath);
        }
        return loveApp.doChat(message,chatId);
    }

    /**
     * 以 SSE 方式调用恋爱助手并返回流式结果。
     */
    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId, String option, String imagePath){
        if (shouldUseTodoDemoOption(option)) {
            return todoDemoApp.stream(message, chatId);
        }
        if (shouldUseSkillsOption(option)) {
            return loveApp.streamWithSkills(message, chatId)
                    .map(this::toJson);
        }
        if (shouldUseImageOption(option, imagePath)) {
            return loveApp.doChatWithImageStream(message, chatId, imagePath);
        }
        return loveApp.doChatWithStream(message,chatId);
    }

    /**
     * 通过 ServerSentEvent 包装流式输出。
     */
    @GetMapping(value = "/love_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> zdoChatWithLoveAppServerSentEvent(String message, String chatId, String option, String imagePath){
        Flux<String> contentFlux = shouldUseSkillsOption(option)
                ? loveApp.streamWithSkills(message, chatId).map(this::toJson)
                : shouldUseImageOption(option, imagePath)
                ? loveApp.doChatWithImageStream(message, chatId, imagePath)
                : loveApp.doChatWithStream(message, chatId);
        return contentFlux
                .map(chunk->ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * 通过 SseEmitter 直接向客户端推送恋爱助手回复。
     */
    @GetMapping(value = "/love_app/chat/sse_emitter", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithLoveAppSseEmitter(String message, String chatId, String option, String imagePath) {
        SseEmitter emitter = new SseEmitter(DEFAULT_SSE_TIMEOUT);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Flux<String> resultFlux = shouldUseSkillsOption(option)
                        ? loveApp.streamWithSkills(message, chatId).map(this::toJson)
                        : shouldUseTodoDemoOption(option)
                        ? todoDemoApp.stream(message, chatId)
                        : shouldUseImageOption(option, imagePath)
                        ? loveApp.doChatWithImageStream(message, chatId, imagePath)
                        : loveApp.doChatWithStream(message, chatId);

                for (String chunk : resultFlux.toIterable()) {
                    emitter.send(SseEmitter.event().name("message").data(chunk));
                }
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                if (!isClientDisconnect(e)) {
                    emitter.completeWithError(e);
                } else {
                    emitter.complete();
                }
            } finally {
                executor.shutdown();
            }
        });
        return emitter;
    }

    private boolean isClientDisconnect(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof IOException) {
                return true;
            }
            if (current instanceof IllegalStateException && current.getMessage() != null) {
                String message = current.getMessage();
                if (message.contains("ResponseBodyEmitter has already completed")
                        || message.contains("Broken pipe")
                        || message.contains("broken pipe")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }


    /**
     * 启动 Manus 智能体流式会话。
     */
    @GetMapping("manus/chat")
    public SseEmitter doChatWithManus(String message, String chatId, String option, String imagePath){
        return manusSessionService.startChatStream(chatId, message);
    }

    /**
     * 提交补充答案后继续执行 Manus 会话。
     */
    @PostMapping(value = "manus/chat/continue", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter continueChatWithManus(@RequestBody ManusContinueRequest request) {
        return manusSessionService.continueChatStream(request.chatId(), request.answers());
    }

    /**
     * 上传恋爱助手聊天中使用的图片文件。
     */
    @PostMapping(value = "/love_app/image/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadLoveAppImage(
            @RequestParam("chatId") String chatId,
            @RequestParam("file") MultipartFile file
    ) {
        if (!StringUtils.hasText(chatId)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "chatId 不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "图片不能为空");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "仅支持图片文件上传");
        }

        try {
            Path uploadDir = Path.of(FileConstant.IMAGE_UPLOAD_DIR);
            Files.createDirectories(uploadDir);

            String extension = resolveExtension(file.getOriginalFilename());
            String fileName = chatId + "+" + UUID.randomUUID().toString().replace("-", "") + extension;
            Path targetPath = uploadDir.resolve(fileName);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            Map<String, String> result = new LinkedHashMap<>();
            result.put("chatId", chatId);
            result.put("fileName", fileName);
            result.put("filePath", targetPath.toString());
            result.put("relativePath", "tmp" + File.separator + "file" + File.separator + fileName);
            return result;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片保存失败");
        }
    }

    private String resolveExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return ".png";
        }
        int index = originalFilename.lastIndexOf('.');
        if (index < 0 || index == originalFilename.length() - 1) {
            return ".png";
        }
        return originalFilename.substring(index);
    }

    private boolean shouldUseImageOption(String option, String imagePath) {
        return "image".equalsIgnoreCase(option) && StringUtils.hasText(imagePath);
    }

    private boolean shouldUseSkillsOption(String option) {
        return "skills".equalsIgnoreCase(option);
    }

    private boolean shouldUseTodoDemoOption(String option) {
        return "todo-demo".equalsIgnoreCase(option);
    }

    private String toJson(LoveApp.SkillChatResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize skill result", e);
        }
    }

    /**
     * Manus 续聊请求对象，封装继续执行时提交的答案集合。
     */
    public record ManusContinueRequest(String chatId, Map<String, String> answers) {}

}
