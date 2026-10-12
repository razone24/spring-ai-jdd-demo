package com.springai.jdd.llm;

import java.util.Arrays;
import java.util.Locale;

/** The LLM providers the demo can run on, selected with {@code LLM_PROVIDER} (property {@code llm.provider}). */
public enum LlmProvider {
    OPENAI,
    OLLAMA;

    static final String PROVIDER_ACTION = "Set LLM_PROVIDER to 'openai' (default) or 'ollama', or unset it.";

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static LlmProvider parse(String raw) {
        if (raw == null) {
            return OPENAI;
        }
        if (raw.isBlank()) {
            throw new LlmProviderNotAvailableException("LLM_PROVIDER is set but empty.", PROVIDER_ACTION);
        }
        return Arrays.stream(values())
                     .filter(provider -> provider.id().equals(raw))
                     .findFirst()
                     .orElseThrow(() -> new LlmProviderNotAvailableException(
                             "LLM_PROVIDER is '" + raw + "', which is not a supported LLM provider (use lowercase).", PROVIDER_ACTION));
    }
}
