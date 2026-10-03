package com.springai.jdd.assistant.audit;

public interface AuditRecorder {

    void record(QueryAudit audit);

    String name();
}
