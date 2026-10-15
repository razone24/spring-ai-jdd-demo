package com.springai.jdd.assistant.audit.metrics;

import com.springai.jdd.assistant.audit.QueryAudit;
import com.springai.jdd.assistant.audit.ToolCallAudit;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static com.springai.jdd.assistant.audit.metrics.Metrics.*;
import static org.assertj.core.api.Assertions.assertThat;

class MetricsAuditRecorderTest {

    private static final String SCHEDULE = "getConferenceSchedule";
    private static final String KNOWLEDGE_BASE = "searchKnowledgeBase";
    private static final String CONFERENCE_SERVER = "conference-info-mcp";
    private static final String RAG_SERVER = "rag-search-mcp";
    private static final String MODEL_ORIGIN = "MODEL";
    private static final String NO_ERROR = "false";
    private static final String MODEL_NAME = "qwen2.5:7b-instruct";
    private static final String UNKNOWN = "unknown";
    private static final String COST_USD = "0.000520";
    private static final int PROMPT_TOKEN_COUNT = 1000;
    private static final int COMPLETION_TOKEN_COUNT = 75;
    private static final int ROUNDS = 2;
    private static final double TWO_CALLS = 2.0;
    private static final double ONE_CALL = 1.0;
    private static final double NO_COST = 0.0;
    private static final long ONE_RECORD = 1L;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MetricsAuditRecorder recorder = new MetricsAuditRecorder(registry);

    @Test
    void shouldPublishEveryMeterOfAnAuditedQuery() {
        recorder.record(audit());

        assertThat(toolCalls(SCHEDULE, CONFERENCE_SERVER)).isEqualTo(TWO_CALLS);
        assertThat(toolCalls(KNOWLEDGE_BASE, RAG_SERVER)).isEqualTo(ONE_CALL);
        assertThat(registry.counter(Name.QUERIES, Tag.OUTCOME, Outcome.ANSWERED).count()).isEqualTo(ONE_CALL);
        assertThat(registry.counter(Name.TOKENS, Tag.MODEL, MODEL_NAME, Tag.TYPE, TokenType.PROMPT).count())
                .isEqualTo(PROMPT_TOKEN_COUNT);
        assertThat(registry.counter(Name.TOKENS, Tag.MODEL, MODEL_NAME, Tag.TYPE, TokenType.COMPLETION).count())
                .isEqualTo(COMPLETION_TOKEN_COUNT);
        assertThat(registry.counter(Name.COST, Tag.MODEL, MODEL_NAME).count())
                .isEqualTo(new BigDecimal(COST_USD).doubleValue());
        assertThat(registry.summary(Name.TOOL_ROUNDS).totalAmount()).isEqualTo(ROUNDS);
        assertThat(registry.timer(Name.QUERY_DURATION, Tag.OUTCOME, Outcome.ANSWERED).count()).isEqualTo(ONE_RECORD);
    }

    @Test
    void shouldCountACacheHitAsItsOwnOutcome() {
        recorder.record(QueryAudit.builder().cacheHit(true).createdAt(Instant.now()).toolCalls(List.of()).build());

        assertThat(registry.counter(Name.QUERIES, Tag.OUTCOME, Outcome.CACHE_HIT).count()).isEqualTo(ONE_CALL);
    }

    @Test
    void shouldTagWhatTheModelLeftUnnamedAsUnknown() {
        recorder.record(unnamedAudit());

        assertThat(registry.counter(Name.TOOL_CALLS, Tag.TOOL, UNKNOWN, Tag.SERVER, UNKNOWN, Tag.ORIGIN, UNKNOWN,
                                    Tag.ERROR, NO_ERROR).count()).isEqualTo(ONE_CALL);
        assertThat(registry.counter(Name.COST, Tag.MODEL, UNKNOWN).count()).isEqualTo(NO_COST);
    }

    private QueryAudit unnamedAudit() {
        return QueryAudit.builder()
                         .toolRounds(ROUNDS)
                         .createdAt(Instant.now())
                         .toolCalls(List.of(callTo(null, null, null)))
                         .build();
    }

    private QueryAudit audit() {
        return QueryAudit.builder()
                         .model(MODEL_NAME)
                         .toolRounds(ROUNDS)
                         .promptTokens(PROMPT_TOKEN_COUNT)
                         .completionTokens(COMPLETION_TOKEN_COUNT)
                         .costUsd(new BigDecimal(COST_USD))
                         .createdAt(Instant.now())
                         .toolCalls(List.of(callTo(SCHEDULE, CONFERENCE_SERVER, MODEL_ORIGIN),
                                            callTo(SCHEDULE, CONFERENCE_SERVER, MODEL_ORIGIN),
                                            callTo(KNOWLEDGE_BASE, RAG_SERVER, MODEL_ORIGIN)))
                         .build();
    }

    private double toolCalls(String tool, String server) {
        return registry.counter(Name.TOOL_CALLS, Tag.TOOL, tool, Tag.SERVER, server, Tag.ORIGIN, MODEL_ORIGIN,
                                Tag.ERROR, NO_ERROR).count();
    }

    private ToolCallAudit callTo(String tool, String server, String origin) {
        return ToolCallAudit.builder()
                            .toolName(tool)
                            .server(server)
                            .origin(origin)
                            .calledAt(Instant.now())
                            .build();
    }
}
