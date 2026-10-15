package com.springai.jdd.llm;

public class LlmProviderNotAvailableException extends RuntimeException {

    private final String action;

    public LlmProviderNotAvailableException(String description, String action) {
        super(description);
        this.action = action;
    }

    public String description() {
        return getMessage();
    }

    public String action() {
        return action;
    }
}
