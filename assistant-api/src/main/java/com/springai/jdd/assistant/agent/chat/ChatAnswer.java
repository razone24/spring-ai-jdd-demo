package com.springai.jdd.assistant.agent.chat;

import com.springai.jdd.assistant.audit.cost.TokenUsage;
import lombok.Builder;

/**
 * @param stoppedEarly     the round budget ran out before the model produced an answer
 * @param groundingRetried the grounding guard sent an answer without a tool result back to the model
 */
@Builder
public record ChatAnswer(String content,
                         TokenUsage usage,
                         int rounds,
                         boolean stoppedEarly,
                         boolean groundingRetried) {
}
