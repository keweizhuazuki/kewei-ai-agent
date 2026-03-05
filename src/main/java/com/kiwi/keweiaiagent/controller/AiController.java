package com.kiwi.keweiaiagent.controller;

import com.kiwi.keweiaiagent.agent.KeweiManus;
import com.kiwi.keweiaiagent.app.LoveApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/ai")
public class AiController {

    private static final long DEFAULT_SSE_TIMEOUT = 0L;
    private static final long TYPEWRITER_DELAY_MILLIS = 35L;

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel ollamaChatModel;


    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message,String chatId){
        return loveApp.doChat(message,chatId);
    }

    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId){
        return loveApp.doChatWithStream(message,chatId);
    }

    @GetMapping(value = "/love_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> zdoChatWithLoveAppServerSentEvent(String message, String chatId){
        return loveApp.doChatWithStream(message,chatId)
                .map(chunk->ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    @GetMapping(value = "/love_app/chat/sse_emitter", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithLoveAppSseEmitter(String message, String chatId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_SSE_TIMEOUT);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                for (String chunk : loveApp.doChatWithStream(message, chatId).toIterable()) {
                    sendChunkAsTypewriter(emitter, chunk);
                }
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            } finally {
                executor.shutdown();
            }
        });
        return emitter;
    }

    private void sendChunkAsTypewriter(SseEmitter emitter, String chunk) throws IOException, InterruptedException {
        for (char c : chunk.toCharArray()) {
            emitter.send(SseEmitter.event().name("message").data(String.valueOf(c)));
            Thread.sleep(TYPEWRITER_DELAY_MILLIS);
        }
    }


    @GetMapping("manus/chat")
    public SseEmitter doChatWithManus(String message){
        return new KeweiManus(allTools, ollamaChatModel).runStream(message);
    }

}
