package com.springai.jdd.assistant.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record QueryRequest(String prompt,
                    @JsonProperty("conversation_id")
                    String conversationId) {
}
