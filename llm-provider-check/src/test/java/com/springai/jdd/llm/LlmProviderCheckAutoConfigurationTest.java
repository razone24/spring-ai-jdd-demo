package com.springai.jdd.llm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.SpringFactoriesLoader;

import static org.assertj.core.api.Assertions.assertThat;

class LlmProviderCheckAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LlmProviderCheckAutoConfiguration.class));

    @Test
    void shouldStopContextBeforeBeansWhenProviderInvalid() {
        runner.withBean(ModelBean.class)
              .withPropertyValues("llm.provider=bogus")
              .run(context -> assertThat(context).getFailure()
                                                 .isInstanceOf(LlmProviderNotAvailableException.class));
    }

    @Test
    void shouldStartWhenProviderValid() {
        runner.withPropertyValues("llm.provider=ollama", "llm.verify-connectivity=false")
              .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void shouldRenderDescriptionAndAction() {
        LlmProviderNotAvailableException cause = new LlmProviderNotAvailableException("d", "a");

        FailureAnalysis analysis = new LlmProviderNotAvailableFailureAnalyzer().analyze(new RuntimeException(cause));

        assertThat(analysis.getDescription()).isEqualTo("d");
        assertThat(analysis.getAction()).isEqualTo("a");
        assertThat(analysis.getCause()).isSameAs(cause);
    }

    @Test
    void shouldRegisterFailureAnalyzer() {
        // Other registered analyzers need constructor arguments the plain loader can't supply; skip those.
        SpringFactoriesLoader.FailureHandler skipOthers = (type, name, failure) -> { };

        assertThat(SpringFactoriesLoader.forDefaultResourceLocation().load(FailureAnalyzer.class, null, skipOthers))
                .hasAtLeastOneElementOfType(LlmProviderNotAvailableFailureAnalyzer.class);
    }

    /** Stands in for a Spring AI model bean: creating it means the check ran too late. */
    static class ModelBean {
        ModelBean() {
            throw new IllegalStateException("model bean created before the provider check");
        }
    }
}
