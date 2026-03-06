package com.kiwi.keweiaiagent.app;
import com.kiwi.keweiaiagent.advisor.MyLoggerAdvisor;
import com.kiwi.keweiaiagent.advisor.ReReadingAdvisor;
import com.kiwi.keweiaiagent.query.QueryPreprocessor;
import com.kiwi.keweiaiagent.rag.factory.loveapp.LoveAppRetrievalAugmentationAdvisorFactory;
import com.kiwi.keweiaiagent.tools.FileOperationTool;
import com.kiwi.keweiaiagent.tools.PdfConvertTool;
import com.kiwi.keweiaiagent.tools.ResourceDownloadTool;
import com.kiwi.keweiaiagent.tools.WebScrapingTool;
import com.kiwi.keweiaiagent.tools.WebSearchTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.ai.rag.Query;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final QueryPreprocessor queryPreprocessor;


    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";

    @Autowired
    public LoveApp(ChatModel ollamaChatModel,
                   ChatMemory chatMemory,
                   QueryPreprocessor queryPreprocessor){
//        chatMemory = MessageWindowChatMemory.builder()
//                .chatMemoryRepository(new InMemoryChatMemoryRepository())
//                .maxMessages(10)
//                .build();
        this.chatMemory = chatMemory;
        this.queryPreprocessor = queryPreprocessor;

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
        this.queryPreprocessor = null;
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
     * AI 基础聊天接口，输入用户消息和会话ID，输出AI回复内容,流式输出版本
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatWithStream(String message, String chatId){
        return chatClient
                .prompt()
                .user(message)
                .advisors(a -> a.param(CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    /**
     * AI 基础聊天接口，输入用户消息和会话ID，输出AI回复内容
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithImage(String message, String chatId, String imagePath){
        FileSystemResource imageResource = new FileSystemResource(imagePath);
        MediaType mediaType = MediaTypeFactory.getMediaType(imageResource)
                .orElse(MediaType.IMAGE_PNG);
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(u -> u.text(message).media(mediaType, imageResource))
                .advisors(a -> a.param(CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        assert chatResponse != null;
        String content = chatResponse.getResult().getOutput().getText();
        return content;
    }

    public Flux<String> doChatWithImageStream(String message, String chatId, String imagePath){
        FileSystemResource imageResource = new FileSystemResource(imagePath);
        MediaType mediaType = MediaTypeFactory.getMediaType(imageResource)
                .orElse(MediaType.IMAGE_PNG);
        return chatClient
                .prompt()
                .user(u -> u.text(message).media(mediaType, imageResource))
                .advisors(a -> a.param(CONVERSATION_ID, chatId))
                .stream()
                .content();
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


    /**
     * 恋爱知识库问答功能
     */

    @Autowired
    @Qualifier("vectorStore")
    private VectorStore vectorStore;

    public String doChatWithRag(String query, String chatId,boolean withQueryReform){
        Query rewrittenQuery = withQueryReform ? queryPreprocessor.rewriteQueryTransform(new Query(query)) : new Query(query);
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(rewrittenQuery.text())
                .advisors(a -> a.param(CONVERSATION_ID, chatId))
                // 应用 RAG 内存知识库问答
//                .advisors(QuestionAnswerAdvisor.builder(loveAppVectorStore).build())
                // 应用 rag 检索增强服务（基于 PgVector 向量存储）
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                // 应用自定义的 RAG 检索增强服务，基于 PgVector 向量存储，并根据用户状态过滤知识库内容
//                .advisors(
//                        LoveAppRetrievalAugmentationAdvisorFactory.createLoveAppRagCustomAdvisor(
//                                vectorStore,"已婚")
//                )
                .call()
                .chatResponse();
        assert chatResponse != null;
        return chatResponse.getResult().getOutput().getText();
    }

    // 工具调用功能
    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String chatId){
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(a -> a.param(CONVERSATION_ID, chatId))
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        log.info("chatResponse: {}", chatResponse);
        assert chatResponse != null;
        return chatResponse.getResult().getOutput().getText();
    }

    // mcp 协议注入
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMCP(String message, String chatId){
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(a -> a.param(CONVERSATION_ID, chatId))
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        log.info("chatResponse: {}", chatResponse);
        assert chatResponse != null;
        return chatResponse.getResult().getOutput().getText();
    }

}
