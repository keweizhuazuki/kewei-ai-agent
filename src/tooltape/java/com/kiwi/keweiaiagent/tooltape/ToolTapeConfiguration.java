package com.kiwi.keweiaiagent.tooltape;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;

/** Included in component scanning only when compiled with the tooltape Maven profile. */
@Configuration(proxyBeanMethods = false)
public class ToolTapeConfiguration {
    @Bean
    public static ToolTapeBeanPostProcessor toolTapeBeanPostProcessor(Environment environment)
            throws IOException {
        return new ToolTapeBeanPostProcessor(environment);
    }
}
