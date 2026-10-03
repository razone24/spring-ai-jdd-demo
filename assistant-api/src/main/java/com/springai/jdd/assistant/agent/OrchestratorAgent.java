package com.springai.jdd.assistant.agent;

import com.springai.jdd.assistant.audit.QueryAuditFactory;
import com.springai.jdd.assistant.audit.QueryAuditor;
import com.springai.jdd.assistant.audit.cost.TokenPricing;
import com.springai.jdd.assistant.harness.CachedAnswer;
import com.springai.jdd.assistant.harness.SemanticCache;
import com.springai.jdd.assistant.harness.ToolRoundLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static lombok.AccessLevel.PACKAGE;

/**
 * The orchestrator agent from the diagram. The harness runs the deterministic steps (1, 3, 4);
 * the model makes the one decision that needs intelligence — which tools to call (2).
 */
@Slf4j
@Service
@RequiredArgsConstructor(access = PACKAGE)
public class OrchestratorAgent {

    static final String ROUND_LIMIT_MESSAGE = "I stopped after the number of lookups this assistant allows for one "
                                              + "question without reaching an answer. Please ask a more specific question.";

    private final ChatClient chatClient;
    private final AgentTurns turns;
    private final SemanticCache semanticCache;
    private final ChatMemory chatMemory;
    private final Today today;
    private final TokenPricing pricing;
    private final QueryAuditor auditor;
    private final QueryAuditFactory auditFactory;
    private final Clock clock;

    public QueryOutcome answer(String question, String conversationId) {
        AgentTurn turn = turns.start(question, conversationId);

        // 1. Semantic cache: a question someone already asked is answered in milliseconds, without the LLM.
        //    Only opening questions — a follow-up ("and where is it?") means nothing outside its conversation.
        Optional<CachedAnswer> cached = turn.isOpeningQuestion()
                                        ? semanticCache.lookup(question, turn.trail())
                                        : Optional.empty();
        if (cached.isPresent()) {
            remember(turn, turn.acceptCachedAnswer(cached.get().answer()));
            return audited(turn);
        }

        // 2. The model decides which MCP tools to call; the advisors keep it bounded and grounded.
        String answer = turn.acceptModelAnswer(askModel(turn));

        // 3. A good opening answer goes back into the cache for the next person who asks.
        if (turn.isWorthCaching()) {
            semanticCache.store(question, answer, turn.trail());
        }

        // 4. Every answer is priced and audited: logs, metrics and the database behind Grafana.
        return audited(turn);
    }

    private String askModel(AgentTurn turn) {
        try {
            return chatClient.prompt()
                             .system(system -> system.param("today", today.resolve()))
                             .user(turn.question())
                             .toolCallbacks(turn.tools())
                             .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, turn.conversationId())
                                                         .param(AgentTurn.KEY, turn))
                             .call()
                             .content();
        } catch (ToolRoundLimitException exception) {
            log.warn("Stopped the tool loop: {}", exception.getMessage());
            turn.markStoppedEarly();
            return ROUND_LIMIT_MESSAGE;
        }
    }

    /** The memory advisor never ran for a cache hit, so the exchange is remembered here. */
    private void remember(AgentTurn turn, String answer) {
        chatMemory.add(turn.conversationId(), List.of(new UserMessage(turn.question()), new AssistantMessage(answer)));
    }

    private QueryOutcome audited(AgentTurn turn) {
        QueryOutcome outcome = turn.outcome();
        QueryOutcome priced = outcome.toBuilder().costUsd(pricing.priceOf(outcome.usage())).build();
        auditor.record(auditFactory.buildFrom(turn.question(), priced, clock.instant()));
        return priced;
    }
}
