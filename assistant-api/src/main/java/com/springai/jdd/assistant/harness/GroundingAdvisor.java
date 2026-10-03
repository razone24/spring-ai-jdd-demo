package com.springai.jdd.assistant.agent.chat.loop;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * Grounding guard. A small model that sees its own earlier answers in the conversation memory is
 * tempted to answer a follow-up "from memory" — and to make up the parts it doesn't remember.
 * When a turn ends without the model having called a single tool, the draft is discarded and the
 * model is sent back once, told to look the facts up first. It sits inside the memory advisor, so
 * the discarded draft is never remembered, and outside the tool loop, so the retry gets a full loop.
 */
@Slf4j
public class GroundingAdvisor implements CallAdvisor {

    public static final String GROUNDING_CHECK = "groundingCheck";
    static final String LOOK_IT_UP = """

            (Assistant harness: your previous draft was discarded because it was not based on a tool \
            result from this turn. Call the tool that holds these facts first, then answer from its result. \
            If no tool can help, say so or call refuse.)""";
    private static final String RETRYING = "Answer was not grounded in a tool result; asking the model to look it up";

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.copy(this).nextCall(request);
        if (!(request.context().get(GROUNDING_CHECK) instanceof GroundingCheck check) || check.modelUsedATool()) {
            return response;
        }
        log.info(RETRYING);
        check.markRetried();
        ChatClientRequest lookItUp = request.mutate()
                                            .prompt(request.prompt().augmentUserMessage(this::withLookItUp))
                                            .build();
        return chain.copy(this).nextCall(lookItUp);
    }

    private UserMessage withLookItUp(UserMessage message) {
        return message.mutate().text(message.getText() + LOOK_IT_UP).build();
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
