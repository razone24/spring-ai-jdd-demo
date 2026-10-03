package com.springai.jdd.assistant.agent;

import com.springai.jdd.assistant.agent.cache.CachedAnswer;
import com.springai.jdd.assistant.agent.cache.SemanticCache;
import com.springai.jdd.assistant.agent.chat.ChatAnswer;
import com.springai.jdd.assistant.agent.chat.ChatService;
import com.springai.jdd.assistant.agent.refusal.RefusalReason;
import com.springai.jdd.assistant.agent.tool.ToolSession;
import com.springai.jdd.assistant.agent.tool.ToolSessionFactory;
import com.springai.jdd.assistant.audit.QueryAuditFactory;
import com.springai.jdd.assistant.audit.QueryAuditor;
import com.springai.jdd.assistant.audit.cost.TokenPricing;
import com.springai.jdd.assistant.audit.cost.TokenUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static lombok.AccessLevel.PACKAGE;
import static org.springframework.util.StringUtils.hasText;

/**
 * One query, end to end. The harness owns the deterministic steps — semantic cache, budgets,
 * refusal resolution, memory and audit — and the model owns the decision of which tools to call.
 */
@Service
@RequiredArgsConstructor(access = PACKAGE)
public class QueryOrchestrator {

    static final String CACHE_MODEL = "semantic-cache";

    private final ChatService chatService;
    private final ToolSessionFactory sessions;
    private final SemanticCache cache;
    private final ChatMemory chatMemory;
    private final QueryAuditor auditor;
    private final QueryAuditFactory auditFactory;
    private final TokenPricing pricing;
    private final Clock clock;

    public QueryOutcome answer(String prompt, String conversationId) {
        String conversation = resolveConversationId(conversationId);
        // A follow-up ("and where is it?") only makes sense in its conversation, so only opening
        // questions are served from — and written to — the shared semantic cache.
        boolean openingQuestion = chatMemory.get(conversation).isEmpty();
        ToolSession session = sessions.open();
        long startedAt = System.nanoTime();
        Optional<CachedAnswer> cached = openingQuestion ? cache.lookup(prompt, session.trail()) : Optional.empty();
        QueryOutcome.QueryOutcomeBuilder outcome = cached.map(hit -> answerFromCache(prompt, conversation, hit))
                                                         .orElseGet(() -> answerWithModel(prompt, conversation,
                                                                                          session, openingQuestion));
        QueryOutcome result = outcome.calls(session.calls())
                                     .conversationId(conversation)
                                     .latencyMillis(Duration.ofNanos(System.nanoTime() - startedAt).toMillis())
                                     .build();
        QueryOutcome priced = result.toBuilder().costUsd(pricing.priceOf(result.usage())).build();
        auditor.record(auditFactory.buildFrom(prompt, priced, clock.instant()));
        return priced;
    }

    private QueryOutcome.QueryOutcomeBuilder answerFromCache(String prompt, String conversation, CachedAnswer hit) {
        chatMemory.add(conversation, List.of(new UserMessage(prompt), new AssistantMessage(hit.answer())));
        return QueryOutcome.builder()
                           .message(hit.answer())
                           .usage(TokenUsage.builder().model(CACHE_MODEL).build())
                           .cacheHit(true);
    }

    private QueryOutcome.QueryOutcomeBuilder answerWithModel(String prompt,
                                                             String conversation,
                                                             ToolSession session,
                                                             boolean openingQuestion) {
        ChatAnswer answer = chatService.ask(prompt, conversation, session);
        Optional<RefusalReason> refusal = session.refusalOf(answer.content());
        String message = refusal.map(RefusalReason::getMessage).orElseGet(answer::content);
        if (openingQuestion && refusal.isEmpty() && !answer.stoppedEarly() && hasText(message)) {
            cache.store(prompt, message, session.trail());
        }
        return QueryOutcome.builder()
                           .message(message)
                           .usage(answer.usage())
                           .toolRounds(answer.rounds())
                           .groundingRetried(answer.groundingRetried())
                           .refused(refusal.isPresent())
                           .refusalReason(refusal.map(RefusalReason::name).orElse(null));
    }

    private String resolveConversationId(String conversationId) {
        return hasText(conversationId) ? conversationId : UUID.randomUUID().toString();
    }
}
