package com.springai.jdd.assistant.audit.cost;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@EnableConfigurationProperties(PricingProperties.class)
public class TokenPricing {

    private static final BigDecimal ONE_MILLION_TOKENS = new BigDecimal(1_000_000);
    private static final String UNPRICED_MODEL = "No price is configured for model {}; its cost is recorded as zero";

    private final Map<String, ModelPrice> pricing;
    private final Set<String> unpriced = ConcurrentHashMap.newKeySet();

    TokenPricing(PricingProperties properties) {
        this.pricing = properties.pricing();
    }

    public BigDecimal priceOf(TokenUsage usage) {
        String model = usage.model();
        ModelPrice price = model == null ? null : pricing.get(model);
        if (price == null) {
            warnOnce(model);
            return BigDecimal.ZERO;
        }
        return costOf(usage.promptTokens(), price.inputPerMillion())
                       .add(costOf(usage.completionTokens(), price.outputPerMillion()));
    }

    private BigDecimal costOf(int tokens, BigDecimal perMillion) {
        return new BigDecimal(tokens).divide(ONE_MILLION_TOKENS).multiply(perMillion);
    }

    private void warnOnce(String model) {
        if (model != null && unpriced.add(model)) {
            log.warn(UNPRICED_MODEL, model);
        }
    }
}
