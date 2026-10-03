package com.springai.jdd.assistant.agent.trail;

import lombok.Builder;

@Builder
public record ToolCall(String name,
                       String server,
                       ToolOrigin origin,
                       Object arguments,
                       String resultPreview,
                       long durationMillis,
                       String error) {
}
