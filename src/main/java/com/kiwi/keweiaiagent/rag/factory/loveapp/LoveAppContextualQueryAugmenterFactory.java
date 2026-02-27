package com.kiwi.keweiaiagent.rag.factory.loveapp;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

/**
 * ContextualQueryAugmenterFactory 用于创建适用于恋爱咨询应用的 ContextualQueryAugmenter 实例。
 * 该工厂方法配置了一个特定的提示模板，当上下文为空时，向用户提供一个预定义的回复，告知用户只能回答恋爱相关的问题，并提供联系信息。
 *
 * @author kiwi
 */
public class LoveAppContextualQueryAugmenterFactory {

    public static ContextualQueryAugmenter createInstance(){
        PromptTemplate promptTemplate = new PromptTemplate("""
                你应该输出下面的内容：
                抱歉，我只能回答恋爱相关的问题，别的没办法帮到您哦，
                有问题可以联系 zkkw 的网站 https://baidu.com/ 进行咨询哦。
                """);
        return ContextualQueryAugmenter.builder()
                //此处如果设置为true，则当上下文为空时，仍然会调用 ai 生成内容；
                //如果设置为 false，则直接使用 emptyContextPromptTemplate 提供的内容作为增强结果。
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(promptTemplate)
                .build();

    }

}
