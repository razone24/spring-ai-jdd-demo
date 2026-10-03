package com.springai.jdd.assistant.agent.trail;

import java.util.ArrayList;
import java.util.List;

/**
 * The ordered record of every tool call made while answering one query — what the UI shows,
 * what the audit persists and what the metrics count.
 */
public class ToolTrail {

    private final List<ToolCall> calls = new ArrayList<>();

    public synchronized void record(ToolCall call) {
        calls.add(call);
    }

    public synchronized List<ToolCall> calls() {
        return List.copyOf(calls);
    }
}
