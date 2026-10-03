package com.springai.jdd.assistant.agent.cache;

import com.springai.jdd.assistant.agent.tool.McpToolset;
import com.springai.jdd.assistant.agent.trail.ToolCall;
import com.springai.jdd.assistant.agent.trail.ToolTrail;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

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
    private static final String MATCHES = """
            [{"question":"wifi password?","answer":"Java4Ever!","score":0.86},
             {"question":"What is the Wi-Fi password?","answer":"It is Java4Ever!","score":0.98}]""";

    private final McpToolset toolset = mock(McpToolset.class);
    private final ToolTrail trail = new ToolTrail();

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
        SemanticCache disabled = new SemanticCache(toolset, new ObjectMapper(), new CacheProperties(false, 0.9, Set.of()));

        assertThat(disabled.lookup(QUESTION, trail)).isEmpty();
        disabled.store(QUESTION, "answer", trail);
        verifyNoInteractions(toolset);
    }

    private SemanticCache cacheWithThreshold(double threshold) {
        return new SemanticCache(toolset, new ObjectMapper(), new CacheProperties(true, threshold, Set.of("searchWeb")));
    }
}
