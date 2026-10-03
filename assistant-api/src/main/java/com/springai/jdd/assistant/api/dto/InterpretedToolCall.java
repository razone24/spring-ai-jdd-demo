package com.springai.jdd.assistant.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InterpretedToolCall(String name,
                                  String server,
                                  String origin,
                                  Object arguments,
                                  @JsonProperty("result_preview")
                                  String resultPreview,
                                  @JsonProperty("duration_ms")
                                  long durationMillis,
                                  String error) {
}
