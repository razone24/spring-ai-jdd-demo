package com.springai.jdd.assistant.agent.cache;

import com.springai.jdd.assistant.agent.tool.McpToolset;
import com.springai.jdd.assistant.agent.trail.ToolTrail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

/**
 * The semantic cache as a deterministic harness step around the model: look up before the first
 * model call, write back after a good answer. A cache that is down only costs a model call — it
 * never fails the query.
 */
@Slf4j
@Component
@EnableConfigurationProperties(CacheProperties.class)
public class SemanticCache {

    static final String SEARCH = "searchChatHistory";
    static final String RECORD = "recordChatHistory";
    private static final String CACHE_UNAVAILABLE = "Semantic cache {} failed, continuing without it: {}";

    private final McpToolset toolset;
    private final ObjectMapper objectMapper;
    private final CacheProperties properties;

    SemanticCache(McpToolset toolset, ObjectMapper objectMapper, CacheProperties properties) {
        this.toolset = toolset;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public Optional<CachedAnswer> lookup(String question, ToolTrail trail) {
        if (!properties.enabled()) {
            return Optional.empty();
        }
        try {
            String matches = toolset.callAsHarness(SEARCH, Map.of("question", question), trail);
            return bestHit(objectMapper.readTree(matches));
        } catch (RuntimeException exception) {
            log.warn(CACHE_UNAVAILABLE, "lookup", exception.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Writes a good answer back — unless it was built from live data or from a tool that failed,
     * because then the next person asking would get a stale or broken answer instantly.
     */
    public void store(String question, String answer, ToolTrail trail) {
        if (!properties.enabled() || !cacheable(trail)) {
            return;
        }
        try {
            toolset.callAsHarness(RECORD, Map.of("question", question, "answer", answer), trail);
        } catch (RuntimeException exception) {
            log.warn(CACHE_UNAVAILABLE, "write-back", exception.getMessage());
        }
    }

    private boolean cacheable(ToolTrail trail) {
        return trail.calls()
                    .stream()
                    .noneMatch(call -> call.error() != null || properties.uncacheableTools().contains(call.name()));
    }

    private Optional<CachedAnswer> bestHit(JsonNode matches) {
        CachedAnswer best = null;
        for (JsonNode match : matches) {
            double score = match.path("score").asDouble();
            if (score >= properties.hitThreshold() && (best == null || score > best.score())) {
                best = new CachedAnswer(match.path("question").asString(), match.path("answer").asString(), score);
            }
        }
        return Optional.ofNullable(best);
    }
}
