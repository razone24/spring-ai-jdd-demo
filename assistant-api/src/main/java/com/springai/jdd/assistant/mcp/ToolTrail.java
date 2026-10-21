package com.springai.jdd.assistant.mcp;

import java.util.ArrayList;
import java.util.List;

/**
 * The ordered record of every tool call made while answering one query — what the UI shows,
 * what the audit persists and what the metrics count. Each call is stamped with the model round
 * it ran in, so the UI can show which calls the model asked for together.
 */
public class ToolTrail {

    private final List<ToolCall> calls = new ArrayList<>();
    private int round;

    public synchronized void enterRound(int round) {
        this.round = round;
    }

    public synchronized void record(ToolCall call) {
        calls.add(call.toBuilder().round(round).build());
    }

    public synchronized List<ToolCall> calls() {
        return List.copyOf(calls);
    }
}
