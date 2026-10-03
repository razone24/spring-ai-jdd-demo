package com.springai.jdd.assistant.agent.refusal;

import java.util.Optional;

public class Refusal {

    private RefusalReason reason;

    public void record(RefusalReason reason) {
        if (this.reason == null && reason != null) {
            this.reason = reason;
        }
    }

    public Optional<RefusalReason> resolveWith(String answerText) {
        return Optional.ofNullable(reason == null ? RefusalText.detect(answerText) : reason);
    }
}
