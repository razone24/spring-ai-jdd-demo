package com.springai.jdd.assistant.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record QueryResponse(String message,
                            @JsonProperty("conversation_id")
                            String conversationId,
                            @JsonProperty("query_interpretation")
                            QueryInterpretation queryInterpretation,
                            ResponseMeta meta) {
}
