package com.springai.jdd.assistant.api.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.local.LocalBucket;

import java.time.Clock;
import java.time.Duration;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

class ClientBuckets {

    private static final Duration REFILL_PERIOD = Duration.ofSeconds(1);
    private static final long ONE_QUERY = 1;

    private final Cache<String, Bucket> buckets;
    private final TimeMeter timeMeter;
    private final int burst;
    private final int perSecond;

    ClientBuckets(Clock clock, RateLimitProperties properties) {
        this.burst = properties.burst();
        this.perSecond = properties.perSecond();
        this.timeMeter = clockTimeMeter(clock);
        this.buckets = Caffeine.newBuilder()
                               .maximumSize(properties.maxClients())
                               .build();
    }

    boolean tryConsumeFor(String client) {
        return buckets.get(client, ignored -> buildBucket())
                      .tryConsume(ONE_QUERY);
    }

    private LocalBucket buildBucket() {
        return Bucket.builder()
                     .addLimit(limit -> limit.capacity(burst).refillGreedy(perSecond, REFILL_PERIOD))
                     .withCustomTimePrecision(timeMeter)
                     .build();
    }

    private TimeMeter clockTimeMeter(Clock clock) {
        return new TimeMeter() {

            @Override
            public long currentTimeNanos() {
                return MILLISECONDS.toNanos(clock.millis());
            }
            @Override
            public boolean isWallClockBased() {
                return true;
            }
        };
    }
}
