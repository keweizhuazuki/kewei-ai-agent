package com.kiwi.keweiaiagent.query;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询预处理器
 * <p>
 * 该组件用于在检索之前对用户查询进行预处理和优化。
 * 提供三种查询处理策略：
 * <ul>
 *   <li>查询重写：优化和改进原始查询表达</li>
 *   <li>查询压缩：压缩和精简查询内容</li>
 *   <li>多查询扩展：将单个查询扩展为多个相关查询</li>
 * </ul>
 * </p>
 *
 * @author kiwi
 */
@Component
public class QueryPreprocessor {

    /**
     * 查询重写转换器
     * 用于重写和优化原始查询
     */
    private final QueryTransformer rewriteQueryTransformer;

    /**
     * 查询压缩转换器
     * 用于压缩和精简查询内容
     */
    private final QueryTransformer compressionQueryTransformer;

    /**
     * 多查询扩展器
     * 用于将单个查询扩展为多个相关查询
     */
    private final MultiQueryExpander multiQueryExpander;

    /**
     * 构造函数
     * <p>
     * 初始化所有查询处理器，包括：
     * <ul>
     *   <li>查询重写转换器</li>
     *   <li>查询压缩转换器</li>
     *   <li>多查询扩展器（默认生成3个查询）</li>
     * </ul>
     * </p>
     *
     * @param chatClientBuilder ChatClient构建器，用于配置各个处理器
     */
    public QueryPreprocessor(ChatClient.Builder chatClientBuilder) {
        this.rewriteQueryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();
        this.compressionQueryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();

        this.multiQueryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .numberOfQueries(3)
                .build();
    }

    /**
     * 重写查询转换
     * <p>
     * 对原始查询进行重写和优化，以提高检索质量。
     * 适用于需要改进查询表达或添加上下文的场景。
     * </p>
     *
     * @param query 原始查询对象
     * @return 重写后的查询对象
     */
    public Query rewriteQueryTransform(Query query) {
        return rewriteQueryTransformer.transform(query);
    }

    /**
     * 压缩查询转换
     * <p>
     * 对查询内容进行压缩和精简，去除冗余信息。
     * 适用于长查询或包含大量历史对话的场景。
     * </p>
     *
     * @param query 原始查询对象
     * @return 压缩后的查询对象
     */
    public Query compressionQueryTransform(Query query) {
        return compressionQueryTransformer.transform(query);
    }

    /**
     * 多查询扩展
     * <p>
     * 将单个查询扩展为多个相关但不同的查询变体。
     * 默认生成3个查询，可以从多个角度检索相关文档，提高召回率。
     * </p>
     *
     * @param query 原始查询对象
     * @return 扩展后的查询列表（包含多个查询变体）
     */
    public List<Query> multiQueryExpand(Query query) {
        return multiQueryExpander.expand(query);
    }
}
