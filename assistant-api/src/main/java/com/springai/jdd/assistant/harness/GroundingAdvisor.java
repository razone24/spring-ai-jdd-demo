package com.springai.jdd.assistant.harness;

import com.springai.jdd.assistant.agent.AgentTurn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

import java.util.Optional;

/**
 * Grounding guard. A small model that sees its earlier answers in memory likes to answer a follow-up
 * "from memory" and make up the parts it forgot. If a turn ends without a single tool call, the draft
 * is thrown away and the model is sent back — once — to look the facts up.
 * <p>
 * Order matters: inside the memory advisor (the discarded draft is never remembered), outside the
 * tool loop (the retry gets a full loop of its own).
 */
@Slf4j
public class GroundingAdvisor implements CallAdvisor {

    static final String LOOK_IT_UP = """

            (Assistant harness: your previous draft was discarded because it was not based on a tool result \
            from this turn. Call the tool that holds these facts first, then answer from its result. If no tool \
            can help, say so or call refuse.)""";

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse draft = chain.copy(this).nextCall(request);

        Optional<AgentTurn> turn = AgentTurn.of(request.context());
        if (turn.isEmpty() || turn.get().modelUsedATool()) {
            return draft;
        }
        log.info("Answer was not grounded in a tool result; asking the model to look it up");
        turn.get().markGroundingRetried();
        return chain.copy(this).nextCall(withLookItUpNote(request));
    }

    private ChatClientRequest withLookItUpNote(ChatClientRequest request) {
        return request.mutate()
                      .prompt(request.prompt().augmentUserMessage(user -> user.mutate()
                                                                              .text(user.getText() + LOOK_IT_UP)
                                                                              .build()))
                      .build();
    }

    @Override
    public String getName() {
        return GroundingAdvisor.class.getSimpleName();
    }

    @Override
    public int getOrder() {
        return ToolCallingAdvisor.DEFAULT_ORDER - 1;
    }
}
