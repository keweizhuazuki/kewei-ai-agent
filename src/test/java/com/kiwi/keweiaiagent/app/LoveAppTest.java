package com.kiwi.keweiaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 发送第一条消息
        String message = "你好，我的名字是Kiwi！";
        String answer = loveApp.doChat( message,chatId);
        // 发送第二条消息
        String message2 = "你喜欢我吗？";
        String answer2 = loveApp.doChat( message2,chatId);
        Assertions.assertNotNull(answer2);
        // 发送第三条消息
        String message3 = "我叫什么名字？帮我回忆一下";
        String answer3 = loveApp.doChat( message3,chatId);
        Assertions.assertNotNull(answer3);
    }

    @Test
    void testGetActorsFilms() {
        LoveApp.ActorsFilms result = loveApp.getActorsFilms("Tom Hanks");

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.actor());
        Assertions.assertNotNull(result.movies());
        Assertions.assertEquals(5, result.movies().size());
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我叫kiwi，我想让另一半（sjr）更爱我，但我不知道该怎么做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }
}
