package com.springai.jdd.assistant.agent;

import com.springai.jdd.assistant.harness.BoundedToolLoopAdvisor;
import com.springai.jdd.assistant.harness.GroundingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.springai.jdd.assistant.configuration.Profiles.PERSISTENCE;

/**
 * Agent = model + harness. The model is whatever {@code spring.ai.model.chat} selects (Ollama by
 * default, or any OpenAI-compatible API); the harness is the system prompt plus three advisors,
 * outermost first: conversation memory → grounding guard → bounded tool loop.
 */
@Configuration
@EnableConfigurationProperties(AgentProperties.class)
class AgentConfiguration {

    @Bean
    ChatClient chatClient(ChatClient.Builder model,
                          ChatMemory chatMemory,
                          ToolCallingManager toolCallingManager,
                          AgentProperties properties) {
        return model.defaultSystem(properties.systemPrompt())
                    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(),
                                     new GroundingAdvisor(),
                                     new BoundedToolLoopAdvisor(toolCallingManager))
                    .build();
    }

    /** Last N messages per conversation, stored in Postgres (JDBC) — or in memory without the persistence profile. */
    @Bean
    ChatMemory chatMemory(ChatMemoryRepository repository, AgentProperties properties) {
        return MessageWindowChatMemory.builder()
                                      .chatMemoryRepository(repository)
                                      .maxMessages(properties.memoryMaxMessages())
                                      .build();
    }

    @Bean
    @Profile("!" + PERSISTENCE)
    ChatMemoryRepository inMemoryChatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }
}
