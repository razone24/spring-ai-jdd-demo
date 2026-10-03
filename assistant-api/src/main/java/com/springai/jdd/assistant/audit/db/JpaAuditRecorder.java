package com.springai.jdd.assistant.audit.db;

import com.springai.jdd.assistant.audit.AuditRecorder;
import com.springai.jdd.assistant.audit.QueryAudit;
import com.springai.jdd.assistant.audit.ToolCallAudit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.springai.jdd.assistant.configuration.Profiles.PERSISTENCE;
import static lombok.AccessLevel.PACKAGE;

@Component
@Profile(PERSISTENCE)
@RequiredArgsConstructor(access = PACKAGE)
public class JpaAuditRecorder implements AuditRecorder {

    private static final String NAME = "jpa";

    private final QueryAuditRepository repository;

    @Override
    @Transactional
    public void record(QueryAudit audit) {
        repository.save(entityOf(audit));
    }

    @Override
    public String name() {
        return NAME;
    }

    private QueryAuditEntity entityOf(QueryAudit audit) {
        return QueryAuditEntity.builder()
                               .requestId(audit.requestId())
                               .conversationId(audit.conversationId())
                               .prompt(audit.prompt())
                               .answer(audit.answer())
                               .cacheHit(audit.cacheHit())
                               .groundingRetried(audit.groundingRetried())
                               .refused(audit.refused())
                               .refusalReason(audit.refusalReason())
                               .model(audit.model())
                               .toolRounds(audit.toolRounds())
                               .promptTokens(audit.promptTokens())
                               .completionTokens(audit.completionTokens())
                               .costUsd(audit.costUsd())
                               .latencyMillis(audit.latencyMillis())
                               .createdAt(audit.createdAt())
                               .toolCalls(callsOf(audit.toolCalls()))
                               .build();
    }

    private List<ToolCallAuditEntity> callsOf(List<ToolCallAudit> calls) {
        return calls.stream()
                    .map(this::entityOf)
                    .toList();
    }

    private ToolCallAuditEntity entityOf(ToolCallAudit call) {
        return ToolCallAuditEntity.builder()
                                  .sequence(call.sequence())
                                  .toolName(call.toolName())
                                  .server(call.server())
                                  .origin(call.origin())
                                  .arguments(call.arguments())
                                  .resultPreview(call.resultPreview())
                                  .durationMillis(call.durationMillis())
                                  .error(call.error())
                                  .calledAt(call.calledAt())
                                  .build();
    }
}
