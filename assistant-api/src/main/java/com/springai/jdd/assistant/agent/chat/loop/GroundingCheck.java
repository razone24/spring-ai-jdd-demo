package com.springai.jdd.assistant.agent.chat.loop;

import com.springai.jdd.assistant.agent.trail.ToolOrigin;
import com.springai.jdd.assistant.agent.trail.ToolTrail;

/**
 * Per-query state of the grounding guard: whether the model looked anything up this turn, and
 * whether the guard had to send it back to do so.
 */
public class GroundingCheck {

    private final ToolTrail trail;
    private boolean retried;

    public GroundingCheck(ToolTrail trail) {
        this.trail = trail;
    }

    boolean modelUsedATool() {
        return trail.calls().stream().anyMatch(call -> call.origin() == ToolOrigin.MODEL);
    }

    void markRetried() {
        retried = true;
    }

    public boolean retried() {
        return retried;
    }
}
