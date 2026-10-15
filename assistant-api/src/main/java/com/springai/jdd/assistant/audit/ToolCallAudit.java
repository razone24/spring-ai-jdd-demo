package com.springai.jdd.assistant.audit;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ToolCallAudit(int sequence,
                            String toolName,
                            String server,
                            String origin,
                            String arguments,
                            String resultPreview,
                            long durationMillis,
                            String error,
                            Instant calledAt) {
}
