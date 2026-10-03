package com.springai.jdd.assistant.harness;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Set;

/**
 * @param hitThreshold     minimum similarity (0..1) for a cached answer to be served without calling the model
 * @param maxAge           older cached answers are ignored, so a changed wiki stops being answered from the past
 * @param uncacheableTools answers that used one of these tools are never cached (live data goes stale)
 */
@Builder
@ConfigurationProperties(prefix = "assistant.cache")
public record CacheProperties(boolean enabled,
                              double hitThreshold,
                              Duration maxAge,
                              Set<String> uncacheableTools) {

    public Set<String> uncacheableTools() {
        return uncacheableTools == null ? Set.of() : Set.copyOf(uncacheableTools);
    }
}
