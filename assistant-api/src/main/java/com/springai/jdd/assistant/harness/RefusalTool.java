package com.springai.jdd.assistant.agent.refusal;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * A guardrail the model calls itself: refusing becomes a typed, auditable event instead of an
 * apology buried in prose. {@code returnDirect} ends the turn without another model round.
 */
@RequiredArgsConstructor
public class RefusalTool {

    private final Refusal refusal;

    @Tool(name = RefusalDescriptors.NAME, description = RefusalDescriptors.DESCRIPTION, returnDirect = true)
    public String refuse(@ToolParam(description = RefusalDescriptors.REASON) RefusalReason reason) {
        refusal.record(reason);
        return reason.getMessage();
    }
}
