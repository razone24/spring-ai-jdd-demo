package com.springai.jdd.assistant.audit.cost;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TokenPricingTest {

    private static final String MODEL = "gpt-4.1-mini";
    private static final String UNKNOWN_MODEL = "qwen2.5:7b-instruct";
    private static final String INPUT_PER_MILLION = "0.40";
    private static final String OUTPUT_PER_MILLION = "1.60";
    private static final String EXPECTED_COST = "0.000520";
    private static final int PROMPT_TOKENS = 1000;
    private static final int COMPLETION_TOKENS = 75;

    private final TokenPricing pricing = new TokenPricing(properties());

    private static PricingProperties properties() {
        return PricingProperties.builder()
                                .pricing(Map.of(MODEL, price()))
                                .build();
    }

    @Test
    void shouldPriceAKnownModel() {
        BigDecimal cost = pricing.priceOf(usageOf(MODEL));

        assertThat(cost).isEqualByComparingTo(new BigDecimal(EXPECTED_COST));
    }

    @Test
    void shouldPriceAnUnknownModelAtZero() {
        assertThat(pricing.priceOf(usageOf(UNKNOWN_MODEL))).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldPriceAnUnnamedModelAtZero() {
        assertThat(pricing.priceOf(usageOf(null))).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private static ModelPrice price() {
        return ModelPrice.builder()
                         .inputPerMillion(new BigDecimal(INPUT_PER_MILLION))
                         .outputPerMillion(new BigDecimal(OUTPUT_PER_MILLION))
                         .build();
    }

    private TokenUsage usageOf(String model) {
        return TokenUsage.builder()
                         .model(model)
                         .promptTokens(PROMPT_TOKENS)
                         .completionTokens(COMPLETION_TOKENS)
                         .build();
    }
}
