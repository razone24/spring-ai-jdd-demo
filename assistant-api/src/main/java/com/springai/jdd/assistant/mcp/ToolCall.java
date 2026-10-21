package com.springai.jdd.assistant.mcp;

import lombok.Builder;

/**
 * One traced tool call. {@code round} is the model round it belongs to: 0 for the harness steps
 * before the tool loop, and the loop's current round for every call made during or after it.
 */
@Builder(toBuilder = true)
public record ToolCall(String name,
                       String server,
                       ToolOrigin origin,
                       Object arguments,
                       String resultPreview,
                       long durationMillis,
                       String error,
                       int round) {
}
