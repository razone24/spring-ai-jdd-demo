package com.springai.jdd.assistant.agent.refusal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RefusalReason {

    OUT_OF_SCOPE("I can only help with JDD 2026 — the conference, its sessions, speakers and venue — and with "
                 + "software engineering topics around it. Please ask me something in that area."),
    UNINTELLIGIBLE("I couldn't understand that. Please rephrase it as a question.");

    private final String message;
}
