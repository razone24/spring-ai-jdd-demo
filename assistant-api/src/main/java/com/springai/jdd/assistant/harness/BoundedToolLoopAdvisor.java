package com.springai.jdd.assistant.harness;

import com.springai.jdd.assistant.agent.AgentTurn;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.model.tool.ToolCallingManager;

/**
 * Spring AI's tool-calling loop with a budget: every model round spends one, and the turn stops when
 * it runs out — a model that keeps calling tools would otherwise loop (and bill) until a timeout.
 * Each round's token usage is tallied on the way out.
 */
public class BoundedToolLoopAdvisor extends ToolCallingAdvisor {

    public BoundedToolLoopAdvisor(ToolCallingManager toolCallingManager) {
        super(toolCallingManager, DEFAULT_TOOL_EXECUTION_ELIGIBILITY_CHECKER, DEFAULT_ORDER, true);
    }

    @Override
    protected ChatClientRequest doBeforeCall(ChatClientRequest request, CallAdvisorChain chain) {
        AgentTurn.of(request.context()).ifPresent(AgentTurn::spendRound);
        return request;
    }

    @Override
    protected ChatClientResponse doAfterCall(ChatClientResponse response, CallAdvisorChain chain) {
        AgentTurn.of(response.context()).ifPresent(turn -> turn.countTokens(response.chatResponse()));
        return response;
    }
}
