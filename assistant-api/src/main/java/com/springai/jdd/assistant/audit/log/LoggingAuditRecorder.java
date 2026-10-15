package com.springai.jdd.assistant.audit.log;

import com.springai.jdd.assistant.audit.AuditRecorder;
import com.springai.jdd.assistant.audit.QueryAudit;
import com.springai.jdd.assistant.audit.ToolCallAudit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.stream.Collectors.joining;

@Slf4j
@Component
class LoggingAuditRecorder implements AuditRecorder {

    private static final String NAME = "logging";
    private static final String AUDIT = "query audited conversation={} cacheHit={} groundingRetried={} tools=[{}] "
                                        + "rounds={} promptTokens={} completionTokens={} costUsd={} latencyMs={} "
                                        + "refused={} reason={}";
    private static final String TOOL_SEPARATOR = ", ";
    private static final String TOOL_FORMAT = "%s@%s(%s) %dms%s";
    private static final String FAILED = " FAILED";

    @Override
    public void record(QueryAudit audit) {
        log.info(AUDIT,
                 audit.conversationId(),
                 audit.cacheHit(),
                 audit.groundingRetried(),
                 describe(audit.toolCalls()),
                 audit.toolRounds(),
                 audit.promptTokens(),
                 audit.completionTokens(),
                 audit.costUsd(),
                 audit.latencyMillis(),
                 audit.refused(),
                 audit.refusalReason());
    }

    @Override
    public String name() {
        return NAME;
    }

    private String describe(List<ToolCallAudit> calls) {
        return calls.stream()
                    .map(call -> TOOL_FORMAT.formatted(call.toolName(), call.server(), call.origin(),
                                                       call.durationMillis(), call.error() == null ? "" : FAILED))
                    .collect(joining(TOOL_SEPARATOR));
    }
}
