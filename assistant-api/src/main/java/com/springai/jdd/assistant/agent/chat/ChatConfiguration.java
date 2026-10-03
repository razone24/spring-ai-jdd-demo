package com.springai.jdd.assistant.agent.chat;

import com.springai.jdd.assistant.agent.chat.loop.GroundingAdvisor;
import com.springai.jdd.assistant.agent.chat.loop.RoundBoundedToolAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChatProperties.class)
class ChatConfiguration {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder,
                          ChatMemory chatMemory,
                          ToolCallingManager toolCallingManager,
                          ChatProperties properties) {
        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                                                                                    .build();
        RoundBoundedToolAdvisor roundBoundedToolAdvisor = new RoundBoundedToolAdvisor(toolCallingManager);
        return builder.defaultSystem(properties.systemPrompt())
                      .defaultAdvisors(messageChatMemoryAdvisor, new GroundingAdvisor(), roundBoundedToolAdvisor)
                      .build();
    }
}
