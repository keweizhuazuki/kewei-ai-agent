package com.kiwi.keweiaiagent.rag;


import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 恋爱大师文档加载器
 */
@Component
class LoveAppDocumentLoader {

    private final Resource[] resources;

    LoveAppDocumentLoader(@Value("classpath:documents/*.md") Resource[] resources) {
        this.resources = resources;
    }

    List<Document> loadMarkdown() {
        List<Document> allDocs = new ArrayList<>();

        for (Resource resource : resources) {
            String filename = resource.getFilename();

            assert filename != null;
            MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                    .withHorizontalRuleCreateDocument(true)
                    .withIncludeCodeBlock(false)
                    .withIncludeBlockquote(false)
                    .withAdditionalMetadata("filename", filename)
                    .build();

            MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
            List<Document> docs = reader.get();

            for (Document doc : docs) {
                doc.getMetadata().put("source", resource.getDescription());
            }

            allDocs.addAll(docs);
        }

        return allDocs;
    }
}

