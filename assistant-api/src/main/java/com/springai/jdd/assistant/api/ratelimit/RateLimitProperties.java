package com.springai.jdd.assistant.api.ratelimit;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Builder
@ConfigurationProperties(prefix = "assistant.rate-limit")
public record RateLimitProperties(int burst,
                                  int perSecond,
                                  int maxClients) {
}
