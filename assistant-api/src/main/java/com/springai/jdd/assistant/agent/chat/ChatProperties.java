package com.springai.jdd.assistant.agent.chat;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@Builder
@ConfigurationProperties(prefix = "assistant.chat")
public record ChatProperties(Resource systemPrompt,
                             String today,
                             int maxToolRounds) {
}
