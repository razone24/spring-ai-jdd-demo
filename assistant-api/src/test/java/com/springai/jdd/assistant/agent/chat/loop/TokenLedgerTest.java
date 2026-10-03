package com.springai.jdd.assistant.agent.chat.loop;

import com.springai.jdd.assistant.audit.cost.TokenUsage;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TokenLedgerTest {

    private static final String MODEL = "gpt-4.1-mini";
    private static final String OTHER_MODEL = "qwen2.5:7b-instruct";
    private static final int FIRST_PROMPT_TOKENS = 120;
    private static final int FIRST_COMPLETION_TOKENS = 30;
    private static final int SECOND_PROMPT_TOKENS = 200;
    private static final int SECOND_COMPLETION_TOKENS = 45;
    private static final int NO_TOKENS = 0;

    private final TokenLedger ledger = new TokenLedger();

    @Test
    void shouldTallyNothingBeforeAnyRound() {
        TokenUsage usage = ledger.tally();

        assertThat(usage.promptTokens()).isEqualTo(NO_TOKENS);
        assertThat(usage.completionTokens()).isEqualTo(NO_TOKENS);
        assertThat(usage.model()).isNull();
    }

    @Test
    void shouldSumEveryRound() {
        ledger.add(responseWith(MODEL, FIRST_PROMPT_TOKENS, FIRST_COMPLETION_TOKENS));
        ledger.add(responseWith(MODEL, SECOND_PROMPT_TOKENS, SECOND_COMPLETION_TOKENS));
        TokenUsage usage = ledger.tally();

        assertThat(usage.promptTokens()).isEqualTo(FIRST_PROMPT_TOKENS + SECOND_PROMPT_TOKENS);
        assertThat(usage.completionTokens()).isEqualTo(FIRST_COMPLETION_TOKENS + SECOND_COMPLETION_TOKENS);
    }

    @Test
    void shouldKeepTheModelOfTheRoundsItSaw() {
        ledger.add(responseWith(MODEL, FIRST_PROMPT_TOKENS, FIRST_COMPLETION_TOKENS));
        ledger.add(responseWith(OTHER_MODEL, SECOND_PROMPT_TOKENS, SECOND_COMPLETION_TOKENS));

        assertThat(ledger.tally().model()).isEqualTo(MODEL);
    }

    @Test
    void shouldIgnoreARoundThatReportsNoUsage() {
        ledger.add(null);
        ledger.add(new ChatResponse(List.of()));
        ledger.add(responseWith(MODEL, FIRST_PROMPT_TOKENS, FIRST_COMPLETION_TOKENS));

        assertThat(ledger.tally().promptTokens()).isEqualTo(FIRST_PROMPT_TOKENS);
    }

    private ChatResponse responseWith(String model, int promptTokens, int completionTokens) {
        return new ChatResponse(List.of(), metadataOf(model, promptTokens, completionTokens));
    }

    private ChatResponseMetadata metadataOf(String model, int promptTokens, int completionTokens) {
        return ChatResponseMetadata.builder()
                                   .model(model)
                                   .usage(new DefaultUsage(promptTokens, completionTokens))
                                   .build();
    }
}
