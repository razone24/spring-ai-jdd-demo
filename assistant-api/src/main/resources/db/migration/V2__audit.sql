CREATE TABLE query_audit (
    request_id        VARCHAR(64)   PRIMARY KEY,
    conversation_id   VARCHAR(64)   NOT NULL,
    prompt            TEXT          NOT NULL,
    answer            TEXT,
    cache_hit         BOOLEAN       NOT NULL DEFAULT FALSE,
    grounding_retried BOOLEAN       NOT NULL DEFAULT FALSE,
    refused           BOOLEAN       NOT NULL DEFAULT FALSE,
    refusal_reason    VARCHAR(32),
    model             VARCHAR(128),
    tool_rounds       INTEGER       NOT NULL DEFAULT 0,
    prompt_tokens     INTEGER       NOT NULL DEFAULT 0,
    completion_tokens INTEGER       NOT NULL DEFAULT 0,
    cost_usd          NUMERIC(12,8) NOT NULL DEFAULT 0,
    latency_ms        BIGINT        NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL
);

CREATE INDEX query_audit_created_at_idx ON query_audit (created_at DESC);
CREATE INDEX query_audit_conversation_idx ON query_audit (conversation_id);

CREATE TABLE tool_call_audit (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_id     VARCHAR(64)  NOT NULL REFERENCES query_audit (request_id) ON DELETE CASCADE,
    sequence       INTEGER      NOT NULL,
    tool_name      VARCHAR(128) NOT NULL,
    server         VARCHAR(128),
    origin         VARCHAR(16),
    arguments      JSONB,
    result_preview TEXT,
    duration_ms    BIGINT       NOT NULL DEFAULT 0,
    error          TEXT,
    called_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX tool_call_audit_called_at_idx ON tool_call_audit (called_at DESC);
CREATE INDEX tool_call_audit_tool_idx ON tool_call_audit (tool_name);
