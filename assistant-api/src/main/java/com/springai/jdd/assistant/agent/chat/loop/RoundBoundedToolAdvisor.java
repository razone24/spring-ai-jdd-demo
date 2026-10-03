package com.springai.jdd.assistant.agent.chat.loop;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.model.tool.ToolCallingManager;

/**
 * Bounds the tool loop and tallies its token use. A model that keeps re-calling tools would
 * otherwise run until the request times out, spending money on every round.
 */
public class RoundBoundedToolAdvisor extends ToolCallingAdvisor {

    public static final String ROUND_BUDGET = "roundBudget";
    public static final String TOKEN_LEDGER = "tokenLedger";
    private static final boolean CONVERSATION_HISTORY_ENABLED = true;

    public RoundBoundedToolAdvisor(ToolCallingManager toolCallingManager) {
        super(toolCallingManager,
              DEFAULT_TOOL_EXECUTION_ELIGIBILITY_CHECKER,
              DEFAULT_ORDER,
              CONVERSATION_HISTORY_ENABLED);
    }

    @Override
    protected ChatClientRequest doBeforeCall(ChatClientRequest request, CallAdvisorChain chain) {
        if (request.context().get(ROUND_BUDGET) instanceof RoundBudget budget) {
            budget.spendRound();
        }
        return request;
    }

    @Override
    protected ChatClientResponse doAfterCall(ChatClientResponse response, CallAdvisorChain chain) {
        if (response.context().get(TOKEN_LEDGER) instanceof TokenLedger ledger) {
            ledger.add(response.chatResponse());
        }
        return response;
    }
}
