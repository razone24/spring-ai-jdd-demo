package com.springai.jdd.assistant.api;

import com.springai.jdd.assistant.agent.QueryOutcome;
import com.springai.jdd.assistant.mcp.ToolCall;
import com.springai.jdd.assistant.api.dto.InterpretedToolCall;
import com.springai.jdd.assistant.api.dto.QueryInterpretation;
import com.springai.jdd.assistant.api.dto.QueryResponse;
import com.springai.jdd.assistant.api.dto.ResponseMeta;
import com.springai.jdd.assistant.audit.RequestId;
import com.springai.jdd.assistant.audit.cost.TokenUsage;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;
import static org.springframework.util.StringUtils.hasText;

@NoArgsConstructor(access = PRIVATE)
public final class QueryResponseFactory {

    public static QueryResponse buildFrom(QueryOutcome outcome) {
        TokenUsage usage = outcome.usage() == null ? TokenUsage.builder().build() : outcome.usage();
        ResponseMeta responseMeta = ResponseMeta.builder()
                                                .requestId(readRequestId())
                                                .model(usage.model())
                                                .promptTokens(usage.promptTokens())
                                                .completionTokens(usage.completionTokens())
                                                .costUsd(outcome.costUsd() == null ? BigDecimal.ZERO : outcome.costUsd())
                                                .rounds(outcome.toolRounds())
                                                .latencyMillis(outcome.latencyMillis())
                                                .cacheHit(outcome.cacheHit())
                                                .groundingRetried(outcome.groundingRetried())
                                                .refused(outcome.refused())
                                                .refusalReason(outcome.refusalReason())
                                                .build();
        return QueryResponse.builder()
                            .message(outcome.message())
                            .conversationId(outcome.conversationId())
                            .queryInterpretation(QueryInterpretation.builder()
                                                                    .tools(interpretedCallsOf(outcome.calls()))
                                                                    .build())
                            .meta(responseMeta)
                            .build();
    }

    public static QueryResponse buildError(String message) {
        return buildFrom(QueryOutcome.builder()
                                     .message(message)
                                     .calls(List.of())
                                     .conversationId(randomId())
                                     .build());
    }

    private static List<InterpretedToolCall> interpretedCallsOf(List<ToolCall> calls) {
        return calls.stream()
                    .map(QueryResponseFactory::interpretedCallOf)
                    .toList();
    }

    private static InterpretedToolCall interpretedCallOf(ToolCall call) {
        return InterpretedToolCall.builder()
                                  .name(call.name())
                                  .server(call.server())
                                  .origin(call.origin() == null ? null : call.origin().name().toLowerCase(Locale.ROOT))
                                  .arguments(call.arguments())
                                  .resultPreview(call.resultPreview())
                                  .durationMillis(call.durationMillis())
                                  .error(call.error())
                                  .build();
    }

    private static String readRequestId() {
        String requestId = RequestId.current();
        return hasText(requestId) ? requestId : randomId();
    }

    private static String randomId() {
        return UUID.randomUUID().toString();
    }
}
