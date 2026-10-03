package com.springai.jdd.assistant.harness;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RoundBudget {

    private static final String ROUND_LIMIT_REACHED = "The query used its %d tool rounds without reaching an answer.";

    private final int maxRounds;
    private int rounds;

    public void spendRound() {
        if (rounds >= maxRounds) {
            throw new ToolRoundLimitException(ROUND_LIMIT_REACHED.formatted(maxRounds));
        }
        rounds++;
    }

    public int rounds() {
        return rounds;
    }
}
