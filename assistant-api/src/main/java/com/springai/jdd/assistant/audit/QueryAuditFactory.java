package com.springai.jdd.assistant.audit;

import com.springai.jdd.assistant.agent.QueryOutcome;
import com.springai.jdd.assistant.agent.trail.ToolCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueryAuditFactory {

    private static final String UNREADABLE_ARGUMENTS = "Could not record the arguments of tool {}";

    private final ObjectMapper objectMapper;

    public QueryAudit buildFrom(String prompt, QueryOutcome outcome, Instant createdAt) {
        return QueryAudit.builder()
                         .requestId(RequestId.current())
                         .conversationId(outcome.conversationId())
                         .prompt(prompt)
                         .answer(outcome.message())
                         .cacheHit(outcome.cacheHit())
                         .groundingRetried(outcome.groundingRetried())
                         .refused(outcome.refused())
                         .refusalReason(outcome.refusalReason())
                         .model(outcome.usage().model())
                         .toolRounds(outcome.toolRounds())
                         .promptTokens(outcome.usage().promptTokens())
                         .completionTokens(outcome.usage().completionTokens())
                         .costUsd(outcome.costUsd())
                         .latencyMillis(outcome.latencyMillis())
                         .createdAt(createdAt)
                         .toolCalls(toolCallsOf(outcome.calls(), createdAt))
                         .build();
    }

    private List<ToolCallAudit> toolCallsOf(List<ToolCall> calls, Instant createdAt) {
        return IntStream.range(0, calls.size())
                        .mapToObj(index -> auditOf(index, calls.get(index), createdAt))
                        .toList();
    }

    private ToolCallAudit auditOf(int sequence, ToolCall call, Instant createdAt) {
        return ToolCallAudit.builder()
                            .sequence(sequence)
                            .toolName(call.name())
                            .server(call.server())
                            .origin(call.origin() == null ? null : call.origin().name())
                            .arguments(argumentsOf(call))
                            .resultPreview(call.resultPreview())
                            .durationMillis(call.durationMillis())
                            .error(call.error())
                            .calledAt(createdAt)
                            .build();
    }

    private String argumentsOf(ToolCall call) {
        try {
            return objectMapper.writeValueAsString(call.arguments());
        } catch (JacksonException exception) {
            log.warn(UNREADABLE_ARGUMENTS, call.name(), exception);
            return null;
        }
    }
}
