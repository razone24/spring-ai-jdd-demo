package com.springai.jdd.assistant.audit.metrics;

import com.springai.jdd.assistant.audit.AuditRecorder;
import com.springai.jdd.assistant.audit.QueryAudit;
import com.springai.jdd.assistant.audit.ToolCallAudit;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.springai.jdd.assistant.audit.metrics.Metrics.*;
import static lombok.AccessLevel.PACKAGE;

@Component
@RequiredArgsConstructor(access = PACKAGE)
class MetricsAuditRecorder implements AuditRecorder {

    private static final String NAME = "metrics";
    private static final String UNKNOWN = "unknown";
    private static final double NO_COST = 0d;

    private final MeterRegistry registry;

    @Override
    public void record(QueryAudit audit) {
        String model = modelOf(audit);
        registry.counter(Name.QUERIES, Tag.OUTCOME, outcomeOf(audit)).increment();
        audit.toolCalls().forEach(this::recordToolCall);
        if (audit.groundingRetried()) {
            registry.counter(Name.GROUNDING_RETRIES).increment();
        }
        registry.summary(Name.TOOL_ROUNDS).record(audit.toolRounds());
        registry.timer(Name.QUERY_DURATION, Tag.OUTCOME, outcomeOf(audit))
                .record(Duration.ofMillis(audit.latencyMillis()));
        registry.counter(Name.TOKENS, Tag.MODEL, model, Tag.TYPE, TokenType.PROMPT).increment(audit.promptTokens());
        registry.counter(Name.TOKENS, Tag.MODEL, model, Tag.TYPE, TokenType.COMPLETION)
                .increment(audit.completionTokens());
        registry.counter(Name.COST, Tag.MODEL, model).increment(costOf(audit));
    }

    @Override
    public String name() {
        return NAME;
    }

    private void recordToolCall(ToolCallAudit call) {
        registry.counter(Name.TOOL_CALLS,
                         Tag.TOOL, valueOrUnknown(call.toolName()),
                         Tag.SERVER, valueOrUnknown(call.server()),
                         Tag.ORIGIN, valueOrUnknown(call.origin()),
                         Tag.ERROR, String.valueOf(call.error() != null))
                .increment();
    }

    private String outcomeOf(QueryAudit audit) {
        if (audit.cacheHit()) {
            return Outcome.CACHE_HIT;
        }
        return audit.refused() ? Outcome.REFUSED : Outcome.ANSWERED;
    }

    private String modelOf(QueryAudit audit) {
        return valueOrUnknown(audit.model());
    }

    private String valueOrUnknown(String value) {
        return value == null ? UNKNOWN : value;
    }

    private double costOf(QueryAudit audit) {
        return audit.costUsd() == null ? NO_COST : audit.costUsd().doubleValue();
    }
}
