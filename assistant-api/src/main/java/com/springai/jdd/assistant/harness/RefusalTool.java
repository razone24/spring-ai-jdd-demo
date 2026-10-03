package com.springai.jdd.assistant.harness;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * A guardrail the model calls itself: refusing becomes a typed, audited event instead of an apology
 * buried in prose. {@code returnDirect} ends the turn without another model round.
 */
@RequiredArgsConstructor
public class RefusalTool {

    public static final String NAME = "refuse";
    public static final String SERVER = "assistant-api";
    private static final String DESCRIPTION = "End the turn when the question cannot be served. Use it instead of "
                                              + "answering anything outside the assistant's scope.";
    private static final String REASON = "OUT_OF_SCOPE: a clear request about something unrelated to the JDD "
                                         + "conference or software engineering — creative writing, personal advice, "
                                         + "politics, sports, cooking, trivia. Practical attendee questions (Wi-Fi "
                                         + "password, food, travel, tickets) are in scope — never refuse those. "
                                         + "UNINTELLIGIBLE: the text forms no question at all — random symbols or "
                                         + "fragments.";

    private final Refusal refusal;

    public static ToolCallback callbackFor(Refusal refusal) {
        return ToolCallbacks.from(new RefusalTool(refusal))[0];
    }

    @Tool(name = NAME, description = DESCRIPTION, returnDirect = true)
    public String refuse(@ToolParam(description = REASON) RefusalReason reason) {
        refusal.record(reason);
        return reason.getMessage();
    }
}
