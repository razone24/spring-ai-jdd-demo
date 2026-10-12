package com.springai.jdd.assistant;

import com.springai.jdd.assistant.harness.CacheProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE)
@TestPropertySource(properties = {
        "llm.provider=ollama", WiringTest.NO_CONNECTIVITY_CHECK, "spring.ai.ollama.init.pull-model-strategy=never"})
class OllamaWiringTest {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private CacheProperties cacheProperties;

    @Test
    void shouldBuildChatClientOnOllama() {
        assertThat(chatClient).isNotNull();
        assertThat(chatModel).isInstanceOf(OllamaChatModel.class);
    }

    @Test
    void shouldUseOllamaCacheThreshold() {
        assertThat(cacheProperties.hitThreshold()).isEqualTo(0.9);
    }
}
