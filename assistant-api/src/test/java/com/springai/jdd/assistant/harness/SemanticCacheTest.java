package com.springai.jdd.assistant.harness;

import com.springai.jdd.assistant.mcp.McpToolset;
import com.springai.jdd.assistant.mcp.ToolCall;
import com.springai.jdd.assistant.mcp.ToolTrail;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SemanticCacheTest {

    private static final String QUESTION = "What is the Wi-Fi password?";
    private static final String NOW = "2026-10-21T09:00:00Z";
    private static final String MATCHES = """
            [{"question":"wifi password?","answer":"Java4Ever!","score":0.86,"timestamp":"2026-10-21T08:00:00Z"},
             {"question":"What is the Wi-Fi password?","answer":"It is Java4Ever!","score":0.98,
              "timestamp":"2026-10-21T08:30:00Z"}]""";
    private static final String STALE_MATCH = """
            [{"question":"What is the Wi-Fi password?","answer":"It was Java4Ever!","score":0.99,
              "timestamp":"2026-10-19T08:30:00Z"}]""";

    private final McpToolset toolset = mock(McpToolset.class);
    private final ToolTrail trail = new ToolTrail();
    private final Clock clock = Clock.fixed(Instant.parse(NOW), ZoneOffset.UTC);

    @Test
    void shouldServeTheClosestMatchAboveTheThreshold() {
        when(toolset.callAsHarness(eq(SemanticCache.SEARCH), anyMap(), any())).thenReturn(MATCHES);

        assertThat(cacheWithThreshold(0.9).lookup(QUESTION, trail))
                .hasValueSatisfying(hit -> {
                    assertThat(hit.answer()).isEqualTo("It is Java4Ever!");
                    assertThat(hit.score()).isEqualTo(0.98);
                });
    }

    @Test
    void shouldMissWhenNothingIsCloseEnough() {
        when(toolset.callAsHarness(eq(SemanticCache.SEARCH), anyMap(), any())).thenReturn(MATCHES);

        assertThat(cacheWithThreshold(0.99).lookup(QUESTION, trail)).isEmpty();
    }

    @Test
    void shouldIgnoreAnAnswerOlderThanTheMaxAge() {
        when(toolset.callAsHarness(eq(SemanticCache.SEARCH), anyMap(), any())).thenReturn(STALE_MATCH);

        assertThat(cacheWithThreshold(0.9).lookup(QUESTION, trail)).isEmpty();
    }

    @Test
    void shouldTreatAnUnavailableCacheAsAMiss() {
        when(toolset.callAsHarness(eq(SemanticCache.SEARCH), anyMap(), any()))
                .thenThrow(new IllegalStateException("connection refused"));

        assertThat(cacheWithThreshold(0.9).lookup(QUESTION, trail)).isEmpty();
    }

    @Test
    void shouldWriteTheAnswerBack() {
        cacheWithThreshold(0.9).store(QUESTION, "Java4Ever!", trail);

        verify(toolset).callAsHarness(SemanticCache.RECORD, Map.of("question", QUESTION, "answer", "Java4Ever!"), trail);
    }

    @Test
    void shouldNotCacheAnAnswerBuiltFromLiveWebResults() {
        trail.record(ToolCall.builder().name("searchWeb").build());

        cacheWithThreshold(0.9).store(QUESTION, "Spring AI 2.0.1", trail);

        verifyNoInteractions(toolset);
    }

    @Test
    void shouldNotCacheAnAnswerBuiltAroundAFailedTool() {
        trail.record(ToolCall.builder().name("searchKnowledgeBase").error("connection refused").build());

        cacheWithThreshold(0.9).store(QUESTION, "I don't know", trail);

        verifyNoInteractions(toolset);
    }

    @Test
    void shouldDoNothingWhenDisabled() {
        SemanticCache disabled = new SemanticCache(toolset, new ObjectMapper(), new CacheProperties(false, 0.9, null, Set.of()), clock);

        assertThat(disabled.lookup(QUESTION, trail)).isEmpty();
        disabled.store(QUESTION, "answer", trail);
        verifyNoInteractions(toolset);
    }

    private SemanticCache cacheWithThreshold(double threshold) {
        return new SemanticCache(toolset, new ObjectMapper(), new CacheProperties(true, threshold, Duration.ofHours(24),
                                                                           Set.of("searchWeb")), clock);
    }
}
