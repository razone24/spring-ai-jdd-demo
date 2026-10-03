package com.springai.jdd.assistant.api.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record QueryInterpretation(List<InterpretedToolCall> tools) {
}
