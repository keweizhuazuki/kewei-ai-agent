package com.kiwi.keweiaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ActiveProfiles({"local"})
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
        String chatId = UUID.randomUUID().toString();

        LoveApp.ActorsFilms result = loveApp.getActorsFilms("Tom Hanks", chatId);

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

    @Test
    void doChatWithImage() {
        String chatId = UUID.randomUUID().toString();
        String message = "这是什么？";
        String answer = loveApp.doChatWithImage(message, chatId, "static/images/test.png");
        Assertions.assertNotNull(answer);
        String message1 = "为什么是东北虎？不是西南虎？";
        String answer1 = loveApp.doChatWithImage(message1, chatId, "static/images/test.png");
        Assertions.assertNotNull(answer1);
    }

    @Test
    void doChatWithRag() {

        String chatId = UUID.randomUUID().toString();
        String message = "结婚后怎么分家务";
        String s = loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(s);
    }

    @Autowired
    @Qualifier("vectorStore")
    private PgVectorStore pgVectorStore;


    @Test
    void testVectorStore(){
        List<Document> documents = List.of(
                new Document("zkkw 是最强的", Map.of("meta1", "value1")),
                new Document("zkkw 是最帅的", Map.of("meta2", "value2")),
                new Document("zkkw 创办的网站 baidu.com 搜索网址是世界第一", Map.of("meta3", "value3")));

        pgVectorStore.add(documents);
        List<Document> results = pgVectorStore.similaritySearch(
                SearchRequest.builder().query("谁创办了 baidu.com").topK(3).build());
        Assertions.assertNotNull(results);
        Assertions.assertFalse(results.isEmpty());
    }
}
