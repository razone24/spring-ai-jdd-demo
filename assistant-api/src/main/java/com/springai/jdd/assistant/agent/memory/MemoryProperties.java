package com.springai.jdd.assistant.agent.memory;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Builder
@ConfigurationProperties(prefix = "assistant.memory")
public record MemoryProperties(int maxMessages) {
}
