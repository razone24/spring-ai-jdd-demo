package com.springai.jdd.assistant.audit.cost;

import lombok.Builder;

@Builder
public record TokenUsage(String model,
                         int promptTokens,
                         int completionTokens) {
}
