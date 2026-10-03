package com.springai.jdd.assistant.agent.tool;

import com.springai.jdd.assistant.agent.refusal.Refusal;
import com.springai.jdd.assistant.agent.refusal.RefusalReason;
import com.springai.jdd.assistant.agent.trail.ToolCall;
import com.springai.jdd.assistant.agent.trail.ToolTrail;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Optional;

/**
 * The per-query view of the toolset: the tools the model may call, the trail they write to and
 * the refusal they may raise. Nothing here outlives the query.
 */
public class ToolSession {

    private final ToolTrail trail;
    private final Refusal refusal;
    private final List<ToolCallback> tools;

    ToolSession(ToolTrail trail, Refusal refusal, List<ToolCallback> tools) {
        this.trail = trail;
        this.refusal = refusal;
        this.tools = List.copyOf(tools);
    }

    public List<ToolCallback> tools() {
        return tools;
    }

    public ToolTrail trail() {
        return trail;
    }

    public List<ToolCall> calls() {
        return trail.calls();
    }

    public Optional<RefusalReason> refusalOf(String answerText) {
        return refusal.resolveWith(answerText);
    }
}
