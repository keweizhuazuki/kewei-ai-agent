package com.kiwi.keweiaiagent.controller;

import com.kiwi.keweiaiagent.app.LoveApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoveApp loveApp;

    @Test
    void shouldStreamTypewriterEffectWithSseEmitter() throws Exception {
        when(loveApp.doChatWithStream(eq("ab"), eq("chat-1"))).thenReturn(Flux.just("ab"));

        MvcResult mvcResult = mockMvc.perform(get("/ai/love_app/chat/sse_emitter")
                        .param("message", "ab")
                        .param("chatId", "chat-1")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        String body = "";
        for (int i = 0; i < 50; i++) {
            body = mvcResult.getResponse().getContentAsString();
            if (body.contains("event:done")) {
                break;
            }
            Thread.sleep(50);
        }

        org.junit.jupiter.api.Assertions.assertTrue(body.contains("event:message"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("data:a"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("data:b"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("event:done"));
    }
}
