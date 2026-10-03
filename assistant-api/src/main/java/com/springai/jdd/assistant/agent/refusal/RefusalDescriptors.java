package com.springai.jdd.assistant.agent.refusal;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class RefusalDescriptors {

    public static final String NAME = "refuse";
    public static final String DESCRIPTION = "End the turn when the question cannot be served. Use it instead of "
                                             + "answering anything outside the assistant's scope.";
    public static final String REASON = "Why the question cannot be served. OUT_OF_SCOPE: a clear request about "
                                        + "something unrelated to the JDD conference or software engineering — "
                                        + "creative writing, personal advice, politics, sports, cooking, trivia. "
                                        + "Practical attendee questions (Wi-Fi password, food, travel, tickets) "
                                        + "are in scope — never refuse those. "
                                        + "UNINTELLIGIBLE: the text forms no question at all — random symbols or "
                                        + "fragments.";
}
