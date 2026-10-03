package com.springai.jdd.assistant.agent;

import com.springai.jdd.assistant.mcp.ToolCall;
import com.springai.jdd.assistant.audit.cost.TokenUsage;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder(toBuilder = true)
public record QueryOutcome(String message,
                           List<ToolCall> calls,
                           String conversationId,
                           TokenUsage usage,
                           BigDecimal costUsd,
                           int toolRounds,
                           long latencyMillis,
                           boolean cacheHit,
                           boolean groundingRetried,
                           boolean refused,
                           String refusalReason) {
}
