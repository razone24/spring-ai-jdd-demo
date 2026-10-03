package com.springai.jdd.assistant.agent;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

import static java.time.ZoneOffset.UTC;
import static org.springframework.util.StringUtils.hasText;

/**
 * The date the model is told is today. Pinning it (NLP_TODAY) makes temporal queries reproducible;
 * every relative period the model resolves — yesterday, last week — is measured from this.
 */
@Component
public class Today {

    private final Clock clock;

    public Today(Clock clock, AgentProperties properties) {
        this.clock = hasText(properties.today()) ? pinTo(properties.today()) : clock;
    }

    LocalDate resolve() {
        return LocalDate.now(clock);
    }

    private static Clock pinTo(String today) {
        return Clock.fixed(LocalDate.parse(today).atStartOfDay(UTC).toInstant(), UTC);
    }
}
