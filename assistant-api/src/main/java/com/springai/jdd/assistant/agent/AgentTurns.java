package com.springai.jdd.assistant.agent;

import com.springai.jdd.assistant.harness.RefusalTool;
import com.springai.jdd.assistant.mcp.McpToolset;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.util.StringUtils.hasText;

/**
 * Starts a turn: resolves the conversation, and hands the model its tools — every MCP tool (traced)
 * plus the local {@code refuse} guardrail.
 */
@Component
@RequiredArgsConstructor
public class AgentTurns {

    private final McpToolset mcpTools;
    private final ChatMemory chatMemory;
    private final AgentProperties properties;

    public AgentTurn start(String question, String conversationId) {
        String conversation = hasText(conversationId) ? conversationId : UUID.randomUUID().toString();
        boolean openingQuestion = chatMemory.get(conversation).isEmpty();
        AgentTurn turn = new AgentTurn(question, conversation, openingQuestion, properties.maxToolRounds());

        List<ToolCallback> tools = new ArrayList<>(mcpTools.toolsFor(turn.trail()));
        tools.add(mcpTools.traced(RefusalTool.callbackFor(turn.refusal()), RefusalTool.SERVER, turn.trail()));
        turn.useTools(tools);
        return turn;
    }
}
