package com.kiwi.keweiaiagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiwi.keweiaiagent.agent.ManusSessionService;
import com.kiwi.keweiaiagent.app.LoveApp;
import com.kiwi.keweiaiagent.app.TodoDemoApp;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
@Import(AiControllerTest.TestBeans.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoveApp loveApp;

    @MockitoBean
    private ChatModel ollamaChatModel;

    @MockitoBean
    private ManusSessionService manusSessionService;

    @MockitoBean
    private TodoDemoApp todoDemoApp;

    @TestConfiguration
    static class TestBeans {

        @Bean
        ToolCallback[] allTools() {
            return new ToolCallback[0];
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

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
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("data:ab"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("event:done"));
    }

    @Test
    void shouldNotWrapSseFluxResponseAsBaseResponse() throws Exception {
        when(loveApp.doChatWithStream(eq("ab"), eq("chat-1"))).thenReturn(Flux.just("ab"));

        MvcResult mvcResult = mockMvc.perform(get("/ai/love_app/chat/sse")
                        .param("message", "ab")
                        .param("chatId", "chat-1")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("data:ab")))
                .andExpect(content().string(not(containsString("\"code\":0"))));
    }

    @Test
    void shouldUseSkillSyncResponseWhenOptionIsSkills() throws Exception {
        when(loveApp.callWithSkills(eq("make ppt"), eq("chat-1")))
                .thenReturn(LoveApp.SkillChatResult.file("/tmp/demo.pptx"));

        mockMvc.perform(get("/ai/love_app/chat/sync")
                        .param("message", "make ppt")
                        .param("chatId", "chat-1")
                        .param("option", "skills"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"type\":\"FILE\"")))
                .andExpect(content().string(containsString("\"filePath\":\"/tmp/demo.pptx\"")));
    }

    @Test
    void shouldUseSkillFluxResponseWhenOptionIsSkills() throws Exception {
        when(loveApp.streamWithSkills(eq("make ppt"), eq("chat-1")))
                .thenReturn(Flux.just(LoveApp.SkillChatResult.question("请补充输出路径")));

        MvcResult mvcResult = mockMvc.perform(get("/ai/love_app/chat/sse")
                        .param("message", "make ppt")
                        .param("chatId", "chat-1")
                        .param("option", "skills")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("\"type\":\"QUESTION\"")))
                .andExpect(content().string(containsString("\"question\":")));
    }

    @Test
    void shouldUseSkillSseEmitterWhenOptionIsSkills() throws Exception {
        when(loveApp.streamWithSkills(eq("make ppt"), eq("chat-1")))
                .thenReturn(Flux.just(LoveApp.SkillChatResult.text("done")));

        MvcResult mvcResult = mockMvc.perform(get("/ai/love_app/chat/sse_emitter")
                        .param("message", "make ppt")
                        .param("chatId", "chat-1")
                        .param("option", "skills")
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

        org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"type\":\"TEXT\""));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("data:{"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("event:done"));
    }

    @Test
    void shouldUseManusSessionServiceForStartChat() throws Exception {
        SseEmitter emitter = new SseEmitter();
        emitter.send(SseEmitter.event().name("question").data("{\"question\":\"what\"}"));
        emitter.send(SseEmitter.event().name("done").data("[DONE]"));
        emitter.complete();
        when(manusSessionService.startChatStream(eq("chat-1"), eq("make one"))).thenReturn(emitter);

        MvcResult mvcResult = mockMvc.perform(get("/ai/manus/chat")
                        .param("message", "make one")
                        .param("chatId", "chat-1")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = "";
        for (int i = 0; i < 50; i++) {
            body = mvcResult.getResponse().getContentAsString();
            if (body.contains("event:done")) {
                break;
            }
            Thread.sleep(50);
        }

        org.junit.jupiter.api.Assertions.assertTrue(body.contains("event:question"));
    }

    @Test
    void shouldUseManusSessionServiceForContinueChat() throws Exception {
        SseEmitter emitter = new SseEmitter();
        emitter.send(SseEmitter.event().name("message").data("continued"));
        emitter.send(SseEmitter.event().name("done").data("[DONE]"));
        emitter.complete();
        when(manusSessionService.continueChatStream(eq("chat-1"), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(emitter);

        MvcResult mvcResult = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/ai/manus/chat/continue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"chatId":"chat-1","answers":{"Q1":"A1"}}
                                """)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = "";
        for (int i = 0; i < 50; i++) {
            body = mvcResult.getResponse().getContentAsString();
            if (body.contains("event:done")) {
                break;
            }
            Thread.sleep(50);
        }

        org.junit.jupiter.api.Assertions.assertTrue(body.contains("data:continued"));
    }

    @Test
    void shouldUseTodoDemoAppWhenOptionIsTodoDemo() throws Exception {
        when(todoDemoApp.stream(eq("拆解一个旅行计划"), eq("chat-1"))).thenReturn(Flux.just("todo-demo"));

        MvcResult mvcResult = mockMvc.perform(get("/ai/love_app/chat/sse_emitter")
                        .param("message", "拆解一个旅行计划")
                        .param("chatId", "chat-1")
                        .param("option", "todo-demo")
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

        org.junit.jupiter.api.Assertions.assertTrue(body.contains("data:todo-demo"));
    }
}
