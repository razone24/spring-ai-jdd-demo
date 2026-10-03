package com.springai.jdd.assistant.audit.cost;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Builder
@ConfigurationProperties(prefix = "assistant")
public record PricingProperties(Map<String, ModelPrice> pricing) {

    public Map<String, ModelPrice> pricing() {
        return pricing == null ? Map.of() : Map.copyOf(pricing);
    }
}
