package com.springai.jdd.assistant.harness;

import com.springai.jdd.assistant.audit.cost.TokenUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

public class TokenLedger {

    private String model;
    private int promptTokens;
    private int completionTokens;

    public void add(ChatResponse response) {
        if (response == null) {
            return;
        }
        Usage usage = response.getMetadata().getUsage();
        promptTokens += valueOf(usage.getPromptTokens());
        completionTokens += valueOf(usage.getCompletionTokens());
        rememberModel(response.getMetadata().getModel());
    }

    public TokenUsage tally() {
        return TokenUsage.builder()
                         .model(model)
                         .promptTokens(promptTokens)
                         .completionTokens(completionTokens)
                         .build();
    }

    private void rememberModel(String reported) {
        if (model == null && reported != null && !reported.isBlank()) {
            model = reported;
        }
    }

    private int valueOf(Integer tokens) {
        return tokens == null ? 0 : tokens;
    }
}
