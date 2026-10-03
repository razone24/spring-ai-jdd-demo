package com.springai.jdd.assistant.agent.cache;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * @param hitThreshold      minimum similarity (0..1) for a cached answer to be served without calling the model
 * @param uncacheableTools  answers that used one of these tools are never cached (live data goes stale)
 */
@Builder
@ConfigurationProperties(prefix = "assistant.cache")
public record CacheProperties(boolean enabled,
                              double hitThreshold,
                              Set<String> uncacheableTools) {

    public Set<String> uncacheableTools() {
        return uncacheableTools == null ? Set.of() : Set.copyOf(uncacheableTools);
    }
}
