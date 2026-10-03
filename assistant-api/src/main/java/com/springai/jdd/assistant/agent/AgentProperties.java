package com.springai.jdd.assistant.agent;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * @param today             pins the date the model is told is today (blank = the real date)
 * @param maxToolRounds     model round trips allowed per question before the tool loop is stopped
 * @param memoryMaxMessages messages per conversation kept in the chat memory window
 */
@Builder
@ConfigurationProperties(prefix = "assistant.agent")
public record AgentProperties(Resource systemPrompt,
                              String today,
                              int maxToolRounds,
                              int memoryMaxMessages) {
}
