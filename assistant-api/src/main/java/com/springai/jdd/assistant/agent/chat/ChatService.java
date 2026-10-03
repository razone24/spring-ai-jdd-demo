package com.springai.jdd.assistant.agent.chat;

import com.springai.jdd.assistant.agent.chat.loop.GroundingCheck;
import com.springai.jdd.assistant.agent.chat.loop.RoundBudget;
import com.springai.jdd.assistant.agent.chat.loop.TokenLedger;
import com.springai.jdd.assistant.agent.chat.loop.ToolRoundLimitException;
import com.springai.jdd.assistant.agent.tool.ToolSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;


import static com.springai.jdd.assistant.agent.chat.loop.GroundingAdvisor.GROUNDING_CHECK;
import static com.springai.jdd.assistant.agent.chat.loop.RoundBoundedToolAdvisor.ROUND_BUDGET;
import static com.springai.jdd.assistant.agent.chat.loop.RoundBoundedToolAdvisor.TOKEN_LEDGER;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    public static final String ROUND_LIMIT_MESSAGE = "I stopped after the number of lookups this assistant allows for "
                                                     + "one question without reaching an answer. Please ask a more "
                                                     + "specific question.";
    private static final String TODAY = "today";
    private static final String STOPPED_THE_LOOP = "Stopped the tool loop: {}";

    private final ChatClient chatClient;
    private final Today today;
    private final ChatProperties properties;

    public ChatAnswer ask(String prompt, String conversationId, ToolSession session) {
        RoundBudget budget = new RoundBudget(properties.maxToolRounds());
        TokenLedger ledger = new TokenLedger();
        GroundingCheck grounding = new GroundingCheck(session.trail());
        ChatAnswer.ChatAnswerBuilder answer = ChatAnswer.builder();
        try {
            answer.content(call(prompt, conversationId, budget, ledger, grounding, session));
        } catch (ToolRoundLimitException exception) {
            log.warn(STOPPED_THE_LOOP, exception.getMessage());
            answer.content(ROUND_LIMIT_MESSAGE).stoppedEarly(true);
        }
        return answer.usage(ledger.tally())
                     .rounds(budget.rounds())
                     .groundingRetried(grounding.retried())
                     .build();
    }

    private String call(String prompt,
                        String conversationId,
                        RoundBudget budget,
                        TokenLedger ledger,
                        GroundingCheck grounding,
                        ToolSession session) {
        return chatClient.prompt()
                         .system(system -> system.param(TODAY, today.resolve()))
                         .user(prompt)
                         .toolCallbacks(session.tools())
                         .advisors(advisor -> advisor.param(CONVERSATION_ID, conversationId)
                                                     .param(ROUND_BUDGET, budget)
                                                     .param(TOKEN_LEDGER, ledger)
                                                     .param(GROUNDING_CHECK, grounding))
                         .call()
                         .content();
    }
}
