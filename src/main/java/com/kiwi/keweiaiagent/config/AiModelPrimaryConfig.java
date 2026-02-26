package com.kiwi.keweiaiagent.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keep multiple AI providers on classpath, but force Ollama as the default selection.
 * <p>
 * Why not just add a new @Primary alias bean:
 * PgVectorStoreAutoConfiguration checks candidates very early and still sees the original
 * two EmbeddingModel beans. Marking the original bean definitions as primary is more stable.
 * <p>
 * Future switch:
 * change the bean names below to DashScope (or another provider), no need to delete starters.
 */
@Configuration
public class AiModelPrimaryConfig {

    @Bean
    public static BeanFactoryPostProcessor ollamaAsPrimaryBeanFactoryPostProcessor() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                markPrimary(beanFactory, "ollamaChatModel");
                markPrimary(beanFactory, "ollamaEmbeddingModel");
            }

            private void markPrimary(ConfigurableListableBeanFactory beanFactory, String beanName) {
                if (beanFactory.containsBeanDefinition(beanName)) {
                    beanFactory.getBeanDefinition(beanName).setPrimary(true);
                }
            }
        };
    }
}
