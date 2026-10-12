package com.springai.jdd.assistant;

import com.springai.jdd.assistant.agent.AgentProperties;
import com.springai.jdd.assistant.audit.cost.TokenPricing;
import com.springai.jdd.assistant.audit.cost.TokenUsage;
import com.springai.jdd.assistant.harness.CacheProperties;
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
@TestPropertySource(properties = {"llm.provider=openai",
        WiringTest.NO_CONNECTIVITY_CHECK, WiringTest.FAKE_OPENAI_KEY, "assistant.agent.today=" + WiringTest.PINNED_DATE})
class WiringTest {

    static final String NO_CONNECTIVITY_CHECK = "llm.verify-connectivity=false";
    static final String FAKE_OPENAI_KEY = "spring.ai.openai.api-key=sk-test-not-real";
    static final String PINNED_DATE = "2025-06-10";

    private static final String PRICED_MODEL = "qwen2.5:7b-instruct";
    private static final String OPENAI_MODEL = "gpt-4o-mini-2024-07-18";
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

    @Autowired
    private CacheProperties cacheProperties;

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

    @Test
    void shouldDefaultToOpenAiCacheThreshold() {
        assertThat(cacheProperties.hitThreshold()).isEqualTo(0.8);
    }

    @Test
    void shouldPriceGpt4oMini() {
        BigDecimal cost = pricing.priceOf(usageOf(OPENAI_MODEL));

        assertThat(cost).isEqualByComparingTo("0.000195");
    }

    private TokenUsage pricedUsage() {
        return usageOf(PRICED_MODEL);
    }

    private TokenUsage usageOf(String model) {
        return TokenUsage.builder()
                         .model(model)
                         .promptTokens(PROMPT_TOKENS)
                         .completionTokens(COMPLETION_TOKENS)
                         .build();
    }
}
