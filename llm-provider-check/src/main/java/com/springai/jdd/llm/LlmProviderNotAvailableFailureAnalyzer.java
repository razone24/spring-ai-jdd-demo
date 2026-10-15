package com.springai.jdd.llm;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

class LlmProviderNotAvailableFailureAnalyzer extends AbstractFailureAnalyzer<LlmProviderNotAvailableException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, LlmProviderNotAvailableException cause) {
        return new FailureAnalysis(cause.description(), cause.action(), cause);
    }
}
