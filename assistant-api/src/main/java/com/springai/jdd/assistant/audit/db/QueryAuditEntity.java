package com.springai.jdd.assistant.audit.db;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@Builder
@Table(name = QueryAuditEntity.TABLE)
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PRIVATE)
class QueryAuditEntity implements Persistable<String> {

    static final String TABLE = "query_audit";
    static final String REQUEST_ID = "request_id";

    @Id
    @Column(name = REQUEST_ID)
    private String requestId;
    private String conversationId;
    private String prompt;
    private String answer;
    private boolean cacheHit;
    private boolean groundingRetried;
    private boolean refused;
    private String refusalReason;
    private String model;
    private int toolRounds;
    private int promptTokens;
    private int completionTokens;
    private BigDecimal costUsd;

    @Column(name = "latency_ms")
    private long latencyMillis;
    private Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = REQUEST_ID, nullable = false)
    private List<ToolCallAuditEntity> toolCalls;

    @Override
    public String getId() {
        return requestId;
    }

    @Override
    public boolean isNew() {
        return true;
    }
}
