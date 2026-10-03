package com.springai.jdd.rag;

/**
 * A retrieved wiki section. {@code similarity} is the cosine similarity when the passage came from the
 * vector search, {@code null} when only the keyword search found it.
 */
public record Passage(String id, String text, String source, Double similarity) {
}
