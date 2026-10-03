package com.springai.jdd.assistant.audit.cost;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ModelPrice(BigDecimal inputPerMillion,
                         BigDecimal outputPerMillion) {
}
