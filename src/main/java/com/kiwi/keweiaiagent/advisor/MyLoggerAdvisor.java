package com.kiwi.keweiaiagent.advisor;

import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;

@Slf4j
/**
 * MyLoggerAdvisor 是一个日志切面（Advisor），实现了 CallAdvisor 和 StreamAdvisor 接口。
 * 它为 Chat Client 的请求和响应提供日志记录功能，支持单次调用和流式调用。
 */
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {


    /**
     * 记录单次 Chat Client 调用的请求和响应日志。
     *
     * @param chatClientRequest 需要记录日志的 Chat Client 请求。
     * @param callAdvisorChain  用于继续执行下一个调用的 Advisor 链。
     * @return 记录日志后的 Chat Client 响应。
     */
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        this.logRequest(chatClientRequest);
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        this.logResponse(chatClientResponse);
        return chatClientResponse;
    }

    /**
     * 记录请求并在流式 Chat Client 调用中聚合响应日志。
     *
     * @param chatClientRequest 需要记录日志的 Chat Client 请求。
     * @param streamAdvisorChain 用于继续执行下一个流式调用的 Advisor 链。
     * @return 记录日志并聚合后的 Chat Client 响应 Flux流。
     */
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        this.logRequest(chatClientRequest);
        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);
        return (new ChatClientMessageAggregator()).aggregateChatClientResponse(chatClientResponses, this::logResponse);
    }

    /**
     * 记录 Chat Client 请求的详细信息。
     *
     * @param request 需要记录日志的 Chat Client 请求。
     */
    protected void logRequest(ChatClientRequest request) {
        Object originalUserText = request.context().get("userText");
        String currentUserText = request.prompt().getUserMessage() == null
                ? null
                : request.prompt().getUserMessage().getText();

        if (originalUserText != null) {
            log.info("original userText: {}", originalUserText);
        }
        log.info("current userText: {}", currentUserText);
        log.info("request contents: {}", request.prompt().getContents());
    }

    /**
     * 记录 Chat Client 响应的详细信息。
     *
     * @param chatClientResponse 需要记录日志的 Chat Client 响应。
     */
    protected void logResponse(ChatClientResponse chatClientResponse) {
        ChatResponse response = chatClientResponse.chatResponse();
        if (response != null) {
            log.info("response: {}", response.getResult().getOutput().getText());
        } else {
            log.info("response is null");
        }
    }

    /**
     * 返回此 Advisor 的名称。
     *
     * @return 此 Advisor 的简单类名。
     */
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 返回此 Advisor 在链中的顺序。
     *
     * @return 顺序值，值越小优先级越高。
     */
    public int getOrder() {
        return 1;
    }
}
