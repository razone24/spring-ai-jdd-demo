package com.springai.jdd.assistant.agent;

import com.springai.jdd.assistant.audit.cost.TokenUsage;
import com.springai.jdd.assistant.harness.Refusal;
import com.springai.jdd.assistant.harness.RefusalReason;
import com.springai.jdd.assistant.harness.RoundBudget;
import com.springai.jdd.assistant.harness.TokenLedger;
import com.springai.jdd.assistant.mcp.ToolOrigin;
import com.springai.jdd.assistant.mcp.ToolTrail;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Everything the harness tracks while answering one question: the tools the model may call, the
 * trail of calls, the round budget, the tokens spent and whether a guard had to step in. The advisors
 * receive it as a request parameter; at the end it becomes the audited {@link QueryOutcome}.
 */
public class AgentTurn {

    public static final String KEY = "agentTurn";
    private static final String CACHE_MODEL = "semantic-cache";
    static final String NO_ANSWER = "I couldn't find an answer to that. Try rephrasing it, or ask about the schedule, "
                                    + "the venue, tickets or the speakers.";

    private final String question;
    private final String conversationId;
    private final boolean openingQuestion;
    private final ToolTrail trail = new ToolTrail();
    private final Refusal refusal = new Refusal();
    private final TokenLedger tokens = new TokenLedger();
    private final RoundBudget rounds;
    private final long startedAt = System.nanoTime();
    private List<ToolCallback> tools = List.of();
    private boolean groundingRetried;
    private boolean stoppedEarly;
    private String answer;
    private boolean cacheHit;
    private Optional<RefusalReason> refused = Optional.empty();
    private boolean blank;

    AgentTurn(String question, String conversationId, boolean openingQuestion, int maxToolRounds) {
        this.question = question;
        this.conversationId = conversationId;
        this.openingQuestion = openingQuestion;
        this.rounds = new RoundBudget(maxToolRounds);
    }

    /** The turn an advisor is working on, read from the request (or response) context. */
    public static Optional<AgentTurn> of(Map<String, Object> context) {
        return context.get(KEY) instanceof AgentTurn turn ? Optional.of(turn) : Optional.empty();
    }

    // ── Harness hooks, called by the advisors ────────────────────────────────────────────────

    public void spendRound() {
        rounds.spendRound();
    }

    public void countTokens(ChatResponse response) {
        tokens.add(response);
    }

    public boolean modelUsedATool() {
        return trail.calls().stream().anyMatch(call -> call.origin() == ToolOrigin.MODEL);
    }

    public void markGroundingRetried() {
        groundingRetried = true;
    }

    // ── The answer ───────────────────────────────────────────────────────────────────────────

    String acceptCachedAnswer(String cached) {
        cacheHit = true;
        answer = cached;
        return answer;
    }

    /** A refusal — by tool call or written out as text — becomes its canonical message; an empty reply a fallback. */
    String acceptModelAnswer(String content) {
        refused = refusal.resolveWith(content);
        blank = content == null || content.isBlank();
        answer = refused.map(RefusalReason::getMessage).orElse(blank ? NO_ANSWER : content);
        return answer;
    }

    /** Only a real answer to an opening question is worth sharing through the semantic cache. */
    boolean isWorthCaching() {
        return openingQuestion && !cacheHit && refused.isEmpty() && !stoppedEarly && !blank;
    }

    /** Snapshot of the turn — taken last, so the trail includes the harness's cache write-back. */
    QueryOutcome outcome() {
        return QueryOutcome.builder()
                           .message(answer)
                           .conversationId(conversationId)
                           .calls(trail.calls())
                           .usage(cacheHit ? TokenUsage.builder().model(CACHE_MODEL).build() : tokens.tally())
                           .toolRounds(rounds.rounds())
                           .latencyMillis(Duration.ofNanos(System.nanoTime() - startedAt).toMillis())
                           .cacheHit(cacheHit)
                           .groundingRetried(groundingRetried)
                           .refused(refused.isPresent())
                           .refusalReason(refused.map(RefusalReason::name).orElse(null))
                           .build();
    }

    // ── Accessors ────────────────────────────────────────────────────────────────────────────

    public String question() {
        return question;
    }

    public String conversationId() {
        return conversationId;
    }

    public boolean isOpeningQuestion() {
        return openingQuestion;
    }

    public ToolTrail trail() {
        return trail;
    }

    public Refusal refusal() {
        return refusal;
    }

    public List<ToolCallback> tools() {
        return tools;
    }

    void useTools(List<ToolCallback> tools) {
        this.tools = List.copyOf(tools);
    }

    void markStoppedEarly() {
        stoppedEarly = true;
    }
}
