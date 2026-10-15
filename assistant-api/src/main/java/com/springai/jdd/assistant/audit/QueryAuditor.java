package com.springai.jdd.assistant.audit;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.springai.jdd.assistant.audit.metrics.Metrics.*;

@Slf4j
@Component
public class QueryAuditor {

    private static final String RECORDER_FAILED = "Audit recorder {} failed for request {}";

    private final List<AuditRecorder> recorders;
    private final MeterRegistry registry;

    QueryAuditor(List<AuditRecorder> recorders, MeterRegistry registry) {
        this.recorders = List.copyOf(recorders);
        this.registry = registry;
    }

    public void record(QueryAudit audit) {
        for (AuditRecorder recorder : recorders) {
            recordSafely(recorder, audit);
        }
    }

    private void recordSafely(AuditRecorder recorder, QueryAudit audit) {
        try {
            recorder.record(audit);
        } catch (RuntimeException exception) {
            log.error(RECORDER_FAILED, recorder.name(), audit.requestId(), exception);
            registry.counter(Name.AUDIT_FAILURES, Tag.RECORDER, recorder.name()).increment();
        }
    }
}
