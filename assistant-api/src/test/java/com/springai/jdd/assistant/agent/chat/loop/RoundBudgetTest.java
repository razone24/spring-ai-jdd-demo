package com.springai.jdd.assistant.agent.chat.loop;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoundBudgetTest {

    private static final int MAX_ROUNDS = 3;

    private final RoundBudget budget = new RoundBudget(MAX_ROUNDS);

    @Test
    void shouldSpendEveryRoundWithinTheCeiling() {
        assertThatCode(this::spendRounds).doesNotThrowAnyException();
    }

    @Test
    void shouldStopTheLoopPastTheCeiling() {
        spendRounds();

        assertThatThrownBy(budget::spendRound)
                .isInstanceOf(ToolRoundLimitException.class)
                .hasMessageContaining(String.valueOf(MAX_ROUNDS));
    }

    @Test
    void shouldReportOnlyRoundsActuallyPerformedAfterBreach() {
        spendRounds();

        assertThatThrownBy(budget::spendRound).isInstanceOf(ToolRoundLimitException.class);
        assertThat(budget.rounds()).isEqualTo(MAX_ROUNDS);
    }

    private void spendRounds() {
        for (int round = 0; round < MAX_ROUNDS; round++) {
            budget.spendRound();
        }
    }
}
