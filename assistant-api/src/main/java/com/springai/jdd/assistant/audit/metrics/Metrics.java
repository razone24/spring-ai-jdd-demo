package com.springai.jdd.assistant.audit.metrics;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class Metrics {

    @NoArgsConstructor(access = PRIVATE)
    public static final class Name {

        public static final String QUERIES = "assistant.queries";
        public static final String TOOL_CALLS = "assistant.tool.calls";
        public static final String TOOL_ROUNDS = "assistant.tool.rounds";
        public static final String QUERY_DURATION = "assistant.query.duration";
        public static final String TOKENS = "assistant.tokens";
        public static final String COST = "assistant.cost.usd";
        public static final String AUDIT_FAILURES = "assistant.audit.failures";
        public static final String GROUNDING_RETRIES = "assistant.grounding.retries";
    }

    @NoArgsConstructor(access = PRIVATE)
    public static final class Tag {

        public static final String TOOL = "tool";
        public static final String SERVER = "server";
        public static final String ORIGIN = "origin";
        public static final String OUTCOME = "outcome";
        public static final String ERROR = "error";
        public static final String MODEL = "model";
        public static final String TYPE = "type";
        public static final String RECORDER = "recorder";
    }

    @NoArgsConstructor(access = PRIVATE)
    public static final class TokenType {

        public static final String PROMPT = "prompt";
        public static final String COMPLETION = "completion";
    }

    @NoArgsConstructor(access = PRIVATE)
    public static final class Outcome {

        public static final String CACHE_HIT = "cache_hit";
        public static final String ANSWERED = "answered";
        public static final String REFUSED = "refused";
    }
}
