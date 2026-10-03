package com.springai.jdd.assistant.agent;

import com.springai.jdd.assistant.agent.cache.CachedAnswer;
import com.springai.jdd.assistant.agent.cache.SemanticCache;
import com.springai.jdd.assistant.agent.chat.ChatProperties;
import com.springai.jdd.assistant.agent.chat.ChatService;
import com.springai.jdd.assistant.agent.chat.Today;
import com.springai.jdd.assistant.agent.chat.loop.GroundingAdvisor;
import com.springai.jdd.assistant.agent.chat.loop.RoundBoundedToolAdvisor;
import com.springai.jdd.assistant.agent.tool.McpToolset;
import com.springai.jdd.assistant.agent.tool.ToolSessionFactory;
import com.springai.jdd.assistant.agent.tool.TracingToolCallback;
import com.springai.jdd.assistant.agent.trail.ToolCall;
import com.springai.jdd.assistant.agent.trail.ToolOrigin;
import com.springai.jdd.assistant.agent.trail.ToolTrail;
import com.springai.jdd.assistant.audit.QueryAudit;
import com.springai.jdd.assistant.audit.QueryAuditFactory;
import com.springai.jdd.assistant.audit.QueryAuditor;
import com.springai.jdd.assistant.audit.ToolCallAudit;
import com.springai.jdd.assistant.audit.cost.TokenPricing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.assertj.core.groups.Tuple;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.springai.jdd.assistant.agent.chat.ChatService.ROUND_LIMIT_MESSAGE;
import static com.springai.jdd.assistant.agent.refusal.RefusalReason.OUT_OF_SCOPE;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueryOrchestratorTest {

    private static final String SCHEDULE_TOOL = "getConferenceSchedule";
    private static final String SCHEDULE_SERVER = "conference-info-mcp";
    private static final String SCHEDULE = "11:00–11:45 | The Future of Software Architecture in the Age of AI — Room B";
    private static final String REFUSE = "refuse";
    private static final String REFUSAL_ARGUMENTS = "{\"reason\":\"OUT_OF_SCOPE\"}";
    private static final String NO_ARGUMENTS = "{}";

    private static final String PROMPT = "When is the talk about AI architecture?";
    private static final String ANSWER = "Wednesday 21 October, 11:00–11:45 in Room B.";
    private static final String FOLLOW_UP = "And who else speaks in that room?";
    private static final String CACHED_ANSWER = "Wednesday, 11:00 in Room B (cached).";
    private static final String CONVERSATION_ID = "conversation-1";
    private static final String SYSTEM_PROMPT = "prompts/system-prompt.txt";
    private static final String SYSTEM_PROMPT_PHRASE = "JDD 2026 conference assistant";
    private static final String NOW = "2026-10-21T08:15:00Z";
    private static final String TODAYS_DATE = "2026-10-21";
    private static final String MODEL = "qwen2.5:7b-instruct";
    private static final int MAX_TOOL_ROUNDS = 3;

    private final ChatModel chatModel = mock(ChatModel.class);
    private final McpToolset toolset = mock(McpToolset.class);
    private final SemanticCache cache = mock(SemanticCache.class);
    private final QueryAuditor auditor = mock(QueryAuditor.class);
    private final TokenPricing pricing = mock(TokenPricing.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock = Clock.fixed(Instant.parse(NOW), UTC);
    private final ChatMemory chatMemory = MessageWindowChatMemory.builder()
                                                                 .chatMemoryRepository(new InMemoryChatMemoryRepository())
                                                                 .maxMessages(20)
                                                                 .build();
    private final ChatService chatService = new ChatService(chatClient(), new Today(clock, chatProperties()),
                                                           chatProperties());
    private final QueryOrchestrator orchestrator = new QueryOrchestrator(chatService,
                                                                         new ToolSessionFactory(toolset, objectMapper),
                                                                         cache,
                                                                         chatMemory,
                                                                         auditor,
                                                                         new QueryAuditFactory(objectMapper),
                                                                         pricing,
                                                                         clock);

    @BeforeEach
    void stubCollaborators() {
        when(chatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        when(toolset.modelTools(any())).thenAnswer(invocation -> List.of(
                new TracingToolCallback(scheduleTool(), SCHEDULE_SERVER, invocation.getArgument(0), objectMapper)));
        when(cache.lookup(anyString(), any())).thenReturn(Optional.empty());
        when(pricing.priceOf(any())).thenReturn(new BigDecimal("0.0001"));
    }

    @Test
    void shouldAnswerWithTheToolTheModelPickedAndTraceIt() {
        stubALookupThenAnAnswer();

        QueryOutcome outcome = orchestrator.answer(PROMPT, null);

        assertThat(outcome.message()).isEqualTo(ANSWER);
        assertThat(outcome.cacheHit()).isFalse();
        assertThat(outcome.calls()).singleElement().satisfies(call -> {
            assertThat(call.name()).isEqualTo(SCHEDULE_TOOL);
            assertThat(call.server()).isEqualTo(SCHEDULE_SERVER);
            assertThat(call.origin()).isEqualTo(ToolOrigin.MODEL);
            assertThat(call.resultPreview()).isEqualTo(SCHEDULE);
        });
    }

    @Test
    void shouldServeAnOpeningQuestionFromTheSemanticCacheWithoutCallingTheModel() {
        when(cache.lookup(eq(PROMPT), any())).thenReturn(Optional.of(new CachedAnswer(PROMPT, CACHED_ANSWER, 0.97)));

        QueryOutcome outcome = orchestrator.answer(PROMPT, CONVERSATION_ID);

        assertThat(outcome.message()).isEqualTo(CACHED_ANSWER);
        assertThat(outcome.cacheHit()).isTrue();
        assertThat(outcome.usage().promptTokens()).isZero();
        assertThat(chatMemory.get(CONVERSATION_ID)).extracting(Message::getText).containsExactly(PROMPT, CACHED_ANSWER);
        verify(chatModel, never()).call(any(Prompt.class));
        verify(cache, never()).store(any(), any(), any());
    }

    @Test
    void shouldWriteAGoodOpeningAnswerBackToTheCache() {
        stubALookupThenAnAnswer();

        orchestrator.answer(PROMPT, CONVERSATION_ID);

        verify(cache).store(eq(PROMPT), eq(ANSWER), any(ToolTrail.class));
    }

    @Test
    void shouldKeepFollowUpsOutOfTheSharedCache() {
        when(chatModel.call(any(Prompt.class))).thenReturn(toolCallResponse(SCHEDULE_TOOL, NO_ARGUMENTS),
                                                          textResponse(ANSWER));
        orchestrator.answer(PROMPT, CONVERSATION_ID);

        orchestrator.answer(FOLLOW_UP, CONVERSATION_ID);

        verify(cache, never()).lookup(eq(FOLLOW_UP), any());
        verify(cache, never()).store(eq(FOLLOW_UP), any(), any());
    }

    @Test
    void shouldReturnTheCanonicalRefusalAndNotCacheIt() {
        when(chatModel.call(any(Prompt.class))).thenReturn(toolCallResponse(REFUSE, REFUSAL_ARGUMENTS),
                                                          textResponse(ANSWER));

        QueryOutcome outcome = orchestrator.answer(PROMPT, null);

        assertThat(outcome.message()).isEqualTo(OUT_OF_SCOPE.getMessage());
        assertThat(outcome.refused()).isTrue();
        assertThat(outcome.refusalReason()).isEqualTo(OUT_OF_SCOPE.name());
        assertThat(outcome.calls()).extracting(ToolCall::name).containsExactly(REFUSE);
        verify(cache, never()).store(any(), any(), any());
    }

    @Test
    void shouldRefuseCanonicallyWhenTheModelWritesTheToolCallAsText() {
        when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("refuse(OUT_OF_SCOPE)"));

        QueryOutcome outcome = orchestrator.answer(PROMPT, null);

        assertThat(outcome.message()).isEqualTo(OUT_OF_SCOPE.getMessage());
        assertThat(outcome.refused()).isTrue();
    }

    @Test
    void shouldStopTheToolLoopAtTheRoundLimitAndNotCacheTheStop() {
        when(chatModel.call(any(Prompt.class))).thenReturn(toolCallResponse(SCHEDULE_TOOL, NO_ARGUMENTS));

        QueryOutcome outcome = orchestrator.answer(PROMPT, null);

        assertThat(outcome.message()).isEqualTo(ROUND_LIMIT_MESSAGE);
        assertThat(outcome.calls()).hasSize(MAX_TOOL_ROUNDS);
        verify(chatModel, times(MAX_TOOL_ROUNDS)).call(any(Prompt.class));
        verify(cache, never()).store(any(), any(), any());
    }

    @Test
    void shouldTallyTokensAcrossRoundsAndAuditThePricedQuery() {
        stubALookupThenAnAnswer();

        QueryOutcome outcome = orchestrator.answer(PROMPT, CONVERSATION_ID);
        QueryAudit audit = auditedQuery();

        assertThat(outcome.usage().promptTokens()).isEqualTo(320);
        assertThat(outcome.usage().completionTokens()).isEqualTo(75);
        assertThat(outcome.toolRounds()).isEqualTo(2);
        assertThat(outcome.costUsd()).isEqualByComparingTo("0.0001");
        assertThat(audit.answer()).isEqualTo(ANSWER);
        assertThat(audit.model()).isEqualTo(MODEL);
        assertThat(audit.toolCalls()).extracting(ToolCallAudit::toolName, ToolCallAudit::server, ToolCallAudit::origin)
                                     .containsExactly(Tuple.tuple(SCHEDULE_TOOL, SCHEDULE_SERVER, "MODEL"));
    }

    @Test
    void shouldSendAnAnswerWithoutAToolResultBackOnceAndRememberOnlyTheGroundedOne() {
        when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("Room B has a talk by someone famous."),
                                                          toolCallResponse(SCHEDULE_TOOL, NO_ARGUMENTS),
                                                          textResponse(ANSWER));

        QueryOutcome outcome = orchestrator.answer(PROMPT, CONVERSATION_ID);

        assertThat(outcome.message()).isEqualTo(ANSWER);
        assertThat(outcome.groundingRetried()).isTrue();
        assertThat(outcome.calls()).extracting(ToolCall::name).containsExactly(SCHEDULE_TOOL);
        assertThat(chatMemory.get(CONVERSATION_ID)).extracting(Message::getText).containsExactly(PROMPT, ANSWER);
    }

    @Test
    void shouldLetAGroundedAnswerThroughUntouched() {
        stubALookupThenAnAnswer();

        assertThat(orchestrator.answer(PROMPT, CONVERSATION_ID).groundingRetried()).isFalse();
        verify(chatModel, times(2)).call(any(Prompt.class));
    }

    @Test
    void shouldGroundThePromptInTheSystemPromptTodayAndTheEarlierTurn() {
        when(chatModel.call(any(Prompt.class))).thenReturn(toolCallResponse(SCHEDULE_TOOL, NO_ARGUMENTS),
                                                          textResponse(ANSWER),
                                                          toolCallResponse(SCHEDULE_TOOL, NO_ARGUMENTS),
                                                          textResponse(ANSWER));

        orchestrator.answer(PROMPT, CONVERSATION_ID);
        orchestrator.answer(FOLLOW_UP, CONVERSATION_ID);
        ArgumentCaptor<Prompt> prompts = forClass(Prompt.class);
        verify(chatModel, times(4)).call(prompts.capture());

        List<String> lastPrompt = prompts.getValue().getInstructions().stream().map(Message::getText).toList();
        assertThat(lastPrompt).anyMatch(text -> text.contains(SYSTEM_PROMPT_PHRASE) && text.contains(TODAYS_DATE));
        assertThat(lastPrompt).contains(PROMPT, ANSWER, FOLLOW_UP);
    }

    @Test
    void shouldMintAConversationIdWhenTheRequestHasNone() {
        when(chatModel.call(any(Prompt.class))).thenReturn(textResponse(ANSWER));

        assertThat(orchestrator.answer(PROMPT, CONVERSATION_ID).conversationId()).isEqualTo(CONVERSATION_ID);
        assertThat(orchestrator.answer(PROMPT, null).conversationId()).isNotBlank();
    }

    private void stubALookupThenAnAnswer() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(toolCallResponse(SCHEDULE_TOOL, NO_ARGUMENTS).getResults(), usageOf(120, 30)),
                            new ChatResponse(textResponse(ANSWER).getResults(), usageOf(200, 45)));
    }

    private QueryAudit auditedQuery() {
        ArgumentCaptor<QueryAudit> captor = forClass(QueryAudit.class);
        verify(auditor).record(captor.capture());
        return captor.getValue();
    }

    private ChatClient chatClient() {
        return ChatClient.builder(chatModel)
                         .defaultSystem(new ClassPathResource(SYSTEM_PROMPT))
                         .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(),
                                          new GroundingAdvisor(),
                                          new RoundBoundedToolAdvisor(ToolCallingManager.builder().build()))
                         .build();
    }

    private static ChatProperties chatProperties() {
        return ChatProperties.builder()
                             .today("")
                             .maxToolRounds(MAX_TOOL_ROUNDS)
                             .build();
    }

    private static ToolCallback scheduleTool() {
        ToolDefinition definition = ToolDefinition.builder()
                                                  .name(SCHEDULE_TOOL)
                                                  .description("The agenda")
                                                  .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                                                  .build();
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return definition;
            }

            @Override
            public String call(String toolInput) {
                return SCHEDULE;
            }
        };
    }

    private ChatResponse toolCallResponse(String name, String arguments) {
        AssistantMessage message = AssistantMessage.builder()
                                                   .content("")
                                                   .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function",
                                                                                                    name, arguments)))
                                                   .build();
        return new ChatResponse(List.of(new Generation(message)));
    }

    private ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private ChatResponseMetadata usageOf(int promptTokens, int completionTokens) {
        return ChatResponseMetadata.builder()
                                   .model(MODEL)
                                   .usage(new DefaultUsage(promptTokens, completionTokens))
                                   .build();
    }
}
