package com.kiwi.keweiaiagent.rag;


import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


/**
 * 向量数据库配置类
 */
@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel ollamaEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(ollamaEmbeddingModel).build();
        List<Document> documents = loveAppDocumentLoader.loadMarkdown();
        simpleVectorStore.add(documents);
        return simpleVectorStore;
    }

}
