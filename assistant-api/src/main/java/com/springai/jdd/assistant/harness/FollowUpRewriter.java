package com.springai.jdd.assistant.harness;

import com.springai.jdd.assistant.agent.AgentTurn;
import com.springai.jdd.assistant.mcp.ToolCall;
import com.springai.jdd.assistant.mcp.ToolOrigin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.joining;

/**
 * Makes a follow-up usable as a semantic-cache key. "And where is it?" means nothing outside its
 * conversation, so one small model call — no tools, no memory — rewrites it into a standalone question
 * ("Where is the talk on software architecture in the age of AI?"). An opening question already stands
 * on its own and costs nothing. A failed rewrite only means the follow-up skips the cache.
 */
@Slf4j
@Component
@EnableConfigurationProperties(CacheProperties.class)
public class FollowUpRewriter {

    static final String TOOL = "rewriteFollowUp";
    static final String SERVER = "assistant-api";
    static final int HISTORY_MESSAGES = 6;
    static final int MAX_MESSAGE_LENGTH = 500;
    static final String INSTRUCTIONS = """
            You rewrite the last question of a conversation with the JDD 2026 conference assistant into one \
            standalone question. Resolve every pronoun and reference ("it", "that room", "the second one") from \
            the conversation, so the question can be understood without it. Keep the user's language and intent. \
            Do not answer it and do not add facts that are not in the conversation. If the question already \
            stands on its own, return it unchanged. Reply with the question only.""";

    private final ChatModel chatModel;
    private final ChatMemory chatMemory;
    private final CacheProperties properties;

    public FollowUpRewriter(ChatModel chatModel, ChatMemory chatMemory, CacheProperties properties) {
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
        this.properties = properties;
    }

    /** The question as the semantic cache should see it — empty when a follow-up can't be rewritten. */
    public Optional<String> standalone(AgentTurn turn) {
        if (turn.isOpeningQuestion()) {
            return Optional.of(turn.question());
        }
        if (!properties.enabled() || !properties.rewriteFollowUps()) {
            return Optional.empty();
        }
        long startedAt = System.nanoTime();
        ToolCall.ToolCallBuilder call = ToolCall.builder()
                                                .name(TOOL)
                                                .server(SERVER)
                                                .origin(ToolOrigin.HARNESS)
                                                .arguments(Map.of("question", turn.question()));
        try {
            ChatResponse response = chatModel.call(promptFor(turn));
            turn.countTokens(response);
            Optional<String> standalone = questionIn(response);
            turn.trail().record(call.resultPreview(standalone.orElse(""))
                                    .durationMillis(elapsed(startedAt))
                                    .build());
            return standalone;
        } catch (RuntimeException exception) {
            log.warn("Could not rewrite the follow-up, it skips the semantic cache: {}", exception.getMessage());
            turn.trail().record(call.error(exception.getMessage()).durationMillis(elapsed(startedAt)).build());
            return Optional.empty();
        }
    }

    private Prompt promptFor(AgentTurn turn) {
        List<Message> history = chatMemory.get(turn.conversationId());
        String conversation = history.subList(Math.max(0, history.size() - HISTORY_MESSAGES), history.size())
                                     .stream()
                                     .map(FollowUpRewriter::line)
                                     .collect(joining("\n"));
        return new Prompt(List.of(new SystemMessage(INSTRUCTIONS),
                                  new UserMessage("Conversation:\n" + conversation
                                                  + "\n\nLast question: " + turn.question())));
    }

    private static String line(Message message) {
        String speaker = message.getMessageType() == MessageType.USER ? "User" : "Assistant";
        String text = message.getText() == null ? "" : message.getText();
        return speaker + ": " + (text.length() > MAX_MESSAGE_LENGTH ? text.substring(0, MAX_MESSAGE_LENGTH) + "…" : text);
    }

    private static Optional<String> questionIn(ChatResponse response) {
        if (response == null || response.getResult() == null) {
            return Optional.empty();
        }
        String text = response.getResult().getOutput().getText();
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(text.strip().lines().findFirst().orElse("").replaceAll("^[\"']|[\"']$", "").strip())
                       .filter(question -> !question.isBlank());
    }

    private static long elapsed(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
