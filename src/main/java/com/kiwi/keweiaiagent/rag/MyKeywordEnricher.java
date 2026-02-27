package com.kiwi.keweiaiagent.rag;


import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于关键词的文档增强器，使用 Ollama 的 ChatModel 来提取关键词并将其添加到文档的元数据中。
 */
@Component
public class MyKeywordEnricher {

    @Resource
    private ChatModel ollamaChatModel;

    public List<Document> enrichDocument(List<Document> documents) {
        KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(ollamaChatModel, 2);
        return keywordMetadataEnricher.apply(documents);
    }
}
