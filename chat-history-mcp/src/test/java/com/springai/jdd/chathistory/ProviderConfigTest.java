package com.springai.jdd.chathistory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderConfigTest {

    @Test
    void shouldDefaultToOpenAi() throws IOException {
        StandardEnvironment env = environment(Map.of());

        assertThat(env.getProperty("spring.ai.model.embedding")).isEqualTo("openai");
        assertThat(env.getProperty("spring.ai.vectorstore.pgvector.table-name")).isEqualTo("chat_history_openai");
        assertThat(env.getProperty("llm.embedding-model")).isEqualTo("text-embedding-3-small");
        assertThat(env.getProperty("spring.ai.openai.embedding.options.dimensions")).isEqualTo("768");
        assertThat(env.getProperty("spring.ai.model.image")).isEqualTo("none");
        assertThat(env.getProperty("chat-history.similarity-threshold")).isEqualTo("0.4");
    }

    @Test
    void shouldSwitchToOllama() throws IOException {
        StandardEnvironment env = environment(Map.of("llm.provider", "ollama"));

        assertThat(env.getProperty("spring.ai.model.embedding")).isEqualTo("ollama");
        assertThat(env.getProperty("spring.ai.vectorstore.pgvector.table-name")).isEqualTo("chat_history_ollama");
        assertThat(env.getProperty("spring.ai.ollama.embedding.model")).isEqualTo("nomic-embed-text");
        assertThat(env.getProperty("chat-history.similarity-threshold")).isEqualTo("0.5");
    }

    @Test
    void shouldLetEnvOverrideThreshold() throws IOException {
        StandardEnvironment env = environment(Map.of("CHAT_HISTORY_SIMILARITY_THRESHOLD", "0.66"));

        assertThat(env.getProperty("chat-history.similarity-threshold")).isEqualTo("0.66");
    }

    @Test
    void shouldEmbedTextOnlyOnOpenAiLikeOllama() throws IOException {
        // OpenAI embeds metadata too by default; Ollama embeds only the text. Same input on both providers.
        StandardEnvironment env = environment(Map.of());

        assertThat(env.getProperty("spring.ai.openai.embedding.metadata-mode")).isEqualTo("none");
    }

    private static StandardEnvironment environment(Map<String, Object> overrides) throws IOException {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        env.getPropertySources().addFirst(new MapPropertySource("overrides", overrides));
        new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yaml"))
                                      .forEach(env.getPropertySources()::addLast);
        return env;
    }
}
