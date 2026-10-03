package com.springai.jdd.assistant.agent.chat;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

class TodayTest {

    private static final String PINNED_DATE = "2025-06-10";
    private static final String NO_DATE = "";
    private static final String SYSTEM_INSTANT = "2026-08-14T10:15:00Z";

    private final Clock systemClock = Clock.fixed(Instant.parse(SYSTEM_INSTANT), UTC);

    @Test
    void shouldResolveTheConfiguredDate() {
        Today today = new Today(systemClock, propertiesFor(PINNED_DATE));

        assertThat(today.resolve()).isEqualTo(LocalDate.parse(PINNED_DATE));
    }

    @Test
    void shouldResolveTheClocksDateWhenNoneIsConfigured() {
        Today today = new Today(systemClock, propertiesFor(NO_DATE));

        assertThat(today.resolve()).isEqualTo(LocalDate.ofInstant(Instant.parse(SYSTEM_INSTANT), UTC));
    }

    private ChatProperties propertiesFor(String today) {
        return ChatProperties.builder()
                             .today(today)
                             .build();
    }
}
