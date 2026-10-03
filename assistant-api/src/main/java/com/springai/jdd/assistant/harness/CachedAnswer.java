package com.springai.jdd.assistant.agent.cache;

public record CachedAnswer(String question,
                           String answer,
                           double score) {
}
