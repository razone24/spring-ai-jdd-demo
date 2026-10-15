package com.springai.jdd.assistant.audit.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@Builder
@Table(name = ToolCallAuditEntity.TABLE)
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PRIVATE)
class ToolCallAuditEntity {

    static final String TABLE = "tool_call_audit";
    private static final String SEQUENCE = "sequence";

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = SEQUENCE)
    private int sequence;
    private String toolName;
    private String server;
    private String origin;

    @JdbcTypeCode(SqlTypes.JSON)
    private String arguments;
    private String resultPreview;

    @Column(name = "duration_ms")
    private long durationMillis;
    private String error;
    private Instant calledAt;
}
