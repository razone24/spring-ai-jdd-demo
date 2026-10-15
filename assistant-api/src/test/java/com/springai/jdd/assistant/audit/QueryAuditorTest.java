package com.springai.jdd.assistant.audit;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.springai.jdd.assistant.audit.metrics.Metrics.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class QueryAuditorTest {

    private static final String REQUEST_ID = "request-1";
    private static final String FAILING_RECORDER = "failing";
    private static final String COUNTING_RECORDER = "counting";
    private static final String RECORDER_FAILED = "recorder failed";
    private static final int BOTH_RECORDERS = 2;
    private static final int THE_SURVIVING_RECORDER = 1;
    private static final double ONE_FAILURE = 1.0;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final List<QueryAudit> recorded = new ArrayList<>();

    @Test
    void shouldPassTheAuditToEveryRecorder() {
        auditorOf(countingRecorder(), countingRecorder()).record(audit());

        assertThat(recorded).hasSize(BOTH_RECORDERS);
    }

    @Test
    void shouldKeepRecordingAndCountTheFailureWhenOneRecorderBreaks() {
        auditorOf(failingRecorder(), countingRecorder()).record(audit());

        assertThat(recorded).hasSize(THE_SURVIVING_RECORDER);
        assertThat(registry.counter(Name.AUDIT_FAILURES, Tag.RECORDER, FAILING_RECORDER).count())
                .isEqualTo(ONE_FAILURE);
    }

    @Test
    void shouldNeverFailTheQueryForAFailedAudit() {
        QueryAuditor auditor = auditorOf(failingRecorder());

        assertThatCode(() -> auditor.record(audit())).doesNotThrowAnyException();
    }

    private QueryAuditor auditorOf(AuditRecorder... recorders) {
        return new QueryAuditor(List.of(recorders), registry);
    }

    private AuditRecorder countingRecorder() {
        return new AuditRecorder() {

            @Override
            public void record(QueryAudit audit) {
                recorded.add(audit);
            }
            @Override
            public String name() {
                return COUNTING_RECORDER;
            }
        };
    }

    private AuditRecorder failingRecorder() {
        return new AuditRecorder() {

            @Override
            public void record(QueryAudit audit) {
                throw new IllegalStateException(RECORDER_FAILED);
            }
            @Override
            public String name() {
                return FAILING_RECORDER;
            }
        };
    }

    private QueryAudit audit() {
        return QueryAudit.builder()
                         .requestId(REQUEST_ID)
                         .createdAt(Instant.now())
                         .toolCalls(List.of())
                         .build();
    }
}
