package com.springai.jdd.assistant.harness;

public record CachedAnswer(String question,
                           String answer,
                           double score) {
}
