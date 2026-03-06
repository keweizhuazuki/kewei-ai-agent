package com.kiwi.keweiaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String message = "婚后家务问题怎么处理？";
        String s = loveApp.doChatWithRag(message, chatId, false);
        Assertions.assertNotNull(s);
    }

    @Test
    void doChatWithRagWithQueryReform() {
        String chatId = UUID.randomUUID().toString();
        String message = "结婚后怎么分家务呜呜呜呜呜,能推荐一些课程么？";
        String s = loveApp.doChatWithRag(message, chatId,true);
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

    @Test
    void doChatWithTools() {
//        // 测试邮件发送工具
//        testMessage("帮我发一封邮件给 shirleysunjr@gmail.com，内容是： 虽然你很笨笨，但我还是很爱你");
//        // 测试时间日期工具
//        testMessage("现在是什么时间？");
//        // 测试联网搜索问题
//        testMessage("周末想带女朋友去上海约会，推荐几个适合情侣的小众打卡地吧");
//
//        // 测试网站抓取
//        testMessage("最近和对象吵架了，看看力扣（leetcode.cn）的其他情侣是怎么解决问题的？");

//        // 测试资源下载
//        testMessage("下载一张适合做手机壁纸的星空情侣图片为文件到/Users/zhukewei/Downloads/dev/codes/kewei-ai-agent/tmp/download");
//
//        // 测试pdf转jpg
//        testMessage("/Users/zhukewei/Downloads/dev/codes/kewei-ai-agent/tmp/pdfs下的所有pdf都转换成jpg图片");
          // 测试制作ppt
        testMessage("""
                    帮我直接调用PptWriterTool生成PPT，不要追问。
                    title=如何让女朋友更爱我
                    outputPath=tmp/ppt/love-guide.pptx
                    slidesMarkdown=
                    ## 送礼物
                    - 选择她真正喜欢的礼物
                    - 仪式感比价格更重要
                    - 结合她最近需求准备小礼物
                    Notes: 强调用心比价格更重要。
                    
                    ## 制造惊喜
                    - 提前准备小而具体的惊喜
                    - 关注她最近的情绪和节奏
                    - 避免让惊喜变成压力
                    Notes: 惊喜要自然贴心。
                    
                    ## 日常相处的注意事项
                    - 多倾听少说教
                    - 尊重边界和情绪
                    - 稳定表达关心和认可
                    Notes: 长期稳定比短期热情更重要。
                    """
                    );
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = loveApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithMCP() throws IOException {
        String chatId = UUID.randomUUID().toString();
//        String message = "我的另一半居住在上海保利越秀，请帮我找到5公里内合适的约会地点";
//        String answer = loveApp.doChatWithTools(message, chatId);
//        Assertions.assertNotNull(answer);
        //测试图片搜索 mcp
//        String message = "帮我搜搜一些哄另一半开心的图片";
//        String answer = loveApp.doChatWithMCP(message, chatId);
//        Assertions.assertNotNull(answer);
        //测试图片生成 mcp
        String message = "帮我生成一张能让另一半开心的图片，风格要可爱，不用太高质量";
        String answer = loveApp.doChatWithMCP(message, chatId);

        Assertions.assertNotNull(answer);

        Path imagePath = Path.of(answer);
        Assertions.assertTrue(Files.exists(imagePath), "图片路径不存在: " + answer);
        Assertions.assertTrue(Files.size(imagePath) > 0, "图片文件为空: " + answer);
        System.out.println("image saved by mcp: " + imagePath.toAbsolutePath());
    }
}
