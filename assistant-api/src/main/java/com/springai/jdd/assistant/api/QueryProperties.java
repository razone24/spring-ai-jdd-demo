package com.springai.jdd.assistant.api;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Builder
@ConfigurationProperties(prefix = "assistant.query")
public record QueryProperties(int maxPromptLength) {
}
