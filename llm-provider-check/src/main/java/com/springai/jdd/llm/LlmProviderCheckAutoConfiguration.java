package com.springai.jdd.llm;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class LlmProviderCheckAutoConfiguration {

    @Bean
    static LlmProviderCheckPostProcessor llmProviderCheckPostProcessor() {
        return new LlmProviderCheckPostProcessor();
    }
}
