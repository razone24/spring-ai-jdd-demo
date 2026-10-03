package com.springai.jdd.assistant.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ResponseMeta(@JsonProperty("request_id")
                           String requestId,
                           String model,
                           @JsonProperty("prompt_tokens")
                           int promptTokens,
                           @JsonProperty("completion_tokens")
                           int completionTokens,
                           @JsonProperty("cost_usd")
                           BigDecimal costUsd,
                           int rounds,
                           @JsonProperty("latency_ms")
                           long latencyMillis,
                           @JsonProperty("cache_hit")
                           boolean cacheHit,
                           @JsonProperty("grounding_retry")
                           boolean groundingRetried,
                           boolean refused,
                           @JsonProperty("refusal_reason")
                           String refusalReason) {
}
