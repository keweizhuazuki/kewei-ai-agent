
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
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {


    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        this.logRequest(chatClientRequest);
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        this.logResponse(chatClientResponse);
        return chatClientResponse;
    }

    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        this.logRequest(chatClientRequest);
        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);
        return (new ChatClientMessageAggregator()).aggregateChatClientResponse(chatClientResponses, this::logResponse);
    }

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

    protected void logResponse(ChatClientResponse chatClientResponse) {
        ChatResponse response = chatClientResponse.chatResponse();
        if (response != null) {
            log.info("response: {}", response.getResult().getOutput().getText());
        } else {
            log.info("response is null");
        }
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }

    public int getOrder() {
        return 1;
    }
}
