package com.springai.jdd.assistant.audit;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record QueryAudit(String requestId,
                         String conversationId,
                         String prompt,
                         String answer,
                         boolean cacheHit,
                         boolean groundingRetried,
                         boolean refused,
                         String refusalReason,
                         String model,
                         int toolRounds,
                         int promptTokens,
                         int completionTokens,
                         BigDecimal costUsd,
                         long latencyMillis,
                         Instant createdAt,
                         List<ToolCallAudit> toolCalls) {
}
