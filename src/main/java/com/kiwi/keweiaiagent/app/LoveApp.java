package com.kiwi.keweiaiagent.app;
import com.kiwi.keweiaiagent.advisor.MyLoggerAdvisor;
import com.kiwi.keweiaiagent.advisor.ReReadingAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.util.List;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;


    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";

    @Autowired
    public LoveApp(ChatModel ollamaChatModel, ChatMemory chatMemory){
//        chatMemory = MessageWindowChatMemory.builder()
//                .chatMemoryRepository(new InMemoryChatMemoryRepository())
//                .maxMessages(10)
//                .build();
        this.chatMemory = chatMemory;

        chatClient = ChatClient.builder(ollamaChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(this.chatMemory).build(),
                        new MyLoggerAdvisor(),
                        new ReReadingAdvisor()
                )
                .build();
    }

    LoveApp(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.chatMemory = null;
    }

    /**
     * AI 基础聊天接口，输入用户消息和会话ID，输出AI回复内容
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId){
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(a -> a.param(CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        assert chatResponse != null;
        String content = chatResponse.getResult().getOutput().getText();
        return content;
    }

    /**
     * AI 基础聊天接口，输入用户消息和会话ID，输出AI回复内容
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithImage(String message, String chatId, String imagePath){
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(u -> u.text(message).media(MimeTypeUtils.IMAGE_PNG,new ClassPathResource(imagePath)))
                .advisors(a -> a.param(CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        assert chatResponse != null;
        String content = chatResponse.getResult().getOutput().getText();
        return content;
    }

    /**
     * 结构化输出转换器
     * 示例：根据演员名字，生成5部电影
     * @param actor
     * @param movies
     */
    public record ActorsFilms(String actor, List<String> movies){}
    public ActorsFilms getActorsFilms(String actor, String chatId){
        return chatClient
                .prompt()
                .user(u -> u
                        .text("Generate 5 movies for {actor}. Return actor and movies.")
                        .param("actor", actor))
                .advisors(a -> a.param(CONVERSATION_ID, chatId))
                .call()
                .entity(ActorsFilms.class);
    }

    public record LoveReport(String title, List<String> suggestions){}
    /**
     * AI 聊天接口，在基础聊天的基础上增加了对AI回复内容的分析和总结，输出最终结果(实战结构化输出）
     * @param message
     * @param chatId
     * @return
     */
    public LoveReport doChatWithReport(String message, String chatId){
        LoveReport  loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(a -> a.param(CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);
        assert loveReport != null;
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

}
