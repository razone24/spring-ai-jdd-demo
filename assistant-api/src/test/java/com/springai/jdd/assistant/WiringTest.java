package com.springai.jdd.assistant;

import com.springai.jdd.assistant.agent.AgentProperties;
import com.springai.jdd.assistant.audit.cost.TokenPricing;
import com.springai.jdd.assistant.audit.cost.TokenUsage;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE)
@TestPropertySource(properties = {WiringTest.NO_MODEL_PULL, "assistant.agent.today=" + WiringTest.PINNED_DATE})
class WiringTest {

    static final String NO_MODEL_PULL = "spring.ai.ollama.init.pull-model-strategy=never";
    static final String PINNED_DATE = "2025-06-10";

    private static final String PRICED_MODEL = "qwen2.5:7b-instruct";
    private static final int PROMPT_TOKENS = 1000;
    private static final int COMPLETION_TOKENS = 75;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private AgentProperties chatProperties;

    @Autowired
    private TokenPricing pricing;

    @Autowired
    private Clock clock;

    @Test
    void shouldBuildTheChatClientTheApplicationAsks() {
        assertThat(chatClient).isNotNull();
    }

    @Test
    void shouldPinTheModelsDateWithoutPinningTheClockTheAuditStampsWith() {
        assertThat(chatProperties.today()).isEqualTo(PINNED_DATE);
        assertThat(clock.instant()).isAfter(LocalDate.parse(PINNED_DATE).atStartOfDay(UTC).toInstant());
    }

    @Test
    void shouldPriceTheModelTheApiReportsDespiteTheDotsInItsKey() {
        BigDecimal cost = pricing.priceOf(pricedUsage());

        assertThat(cost).isGreaterThan(BigDecimal.ZERO);
    }

    private TokenUsage pricedUsage() {
        return TokenUsage.builder()
                         .model(PRICED_MODEL)
                         .promptTokens(PROMPT_TOKENS)
                         .completionTokens(COMPLETION_TOKENS)
                         .build();
    }
}
