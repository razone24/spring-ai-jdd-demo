package com.springai.jdd.assistant.api.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

class ClientBucketsTest {

    private static final String NOW = "2026-08-13T10:15:00Z";
    private static final String CLIENT = "10.0.0.1";
    private static final String OTHER_CLIENT = "10.0.0.2";
    private static final int BURST = 3;
    private static final int PER_SECOND = 1;
    private static final int MAX_CLIENTS = 2;

    private final MutableClock clock = new MutableClock(Instant.parse(NOW));
    private final ClientBuckets buckets = new ClientBuckets(clock, properties());

    private static RateLimitProperties properties() {
        return RateLimitProperties.builder()
                                  .burst(BURST)
                                  .perSecond(PER_SECOND)
                                  .maxClients(MAX_CLIENTS)
                                  .build();
    }

    @Test
    void shouldAllowTheWholeBurstAndNothingBeyondIt() {
        assertThat(consume()).isTrue();
        assertThat(buckets.tryConsumeFor(CLIENT)).isFalse();
    }

    @Test
    void shouldRefillOverTime() {
        consume();
        clock.advance(Duration.ofSeconds(PER_SECOND));

        assertThat(buckets.tryConsumeFor(CLIENT)).isTrue();
    }

    @Test
    void shouldSpendEachClientsBurstSeparately() {
        consume();

        assertThat(buckets.tryConsumeFor(OTHER_CLIENT)).isTrue();
    }

    private boolean consume() {
        boolean allowed = true;
        for (int query = 0; query < BURST; query++) {
            allowed &= buckets.tryConsumeFor(CLIENT);
        }
        return allowed;
    }

    private static final class MutableClock extends Clock {

        private Instant instant;
        private MutableClock(Instant instant) {
            this.instant = instant;
        }
        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
        @Override
        public ZoneId getZone() {
            return UTC;
        }
        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
        @Override
        public Instant instant() {
            return instant;
        }
    }
}
