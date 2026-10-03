package com.springai.jdd.assistant.api;

import com.springai.jdd.assistant.agent.QueryOutcome;
import com.springai.jdd.assistant.agent.OrchestratorAgent;
import com.springai.jdd.assistant.mcp.ToolCall;
import com.springai.jdd.assistant.mcp.ToolOrigin;
import com.springai.jdd.assistant.audit.cost.TokenUsage;
import com.springai.jdd.assistant.configuration.CoreConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.springai.jdd.assistant.api.http.RequestIdFilter.REQUEST_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueryController.class)
@Import(CoreConfiguration.class)
class QueryControllerTest {

    private static final String QUERY_PATH = "/query";
    private static final String REQUEST_BODY = "{\"prompt\":\"When is the Spring AI talk?\"}";
    private static final String BLANK_PROMPT_BODY = "{\"prompt\":\" \"}";
    private static final String MALFORMED_BODY = "{";
    private static final int MAX_PROMPT_LENGTH = 2000;

    private static final String ANSWER = "Spring Boot 4 and Spring AI 2 in Practice — Tuesday 13:00, Room A.";
    private static final String CONVERSATION_ID = "conversation-1";
    private static final String SCHEDULE_TOOL = "getConferenceSchedule";
    private static final String SCHEDULE_SERVER = "conference-info-mcp";
    private static final String MODEL = "qwen2.5:7b-instruct";
    private static final String CALLER_REQUEST_ID = "caller-request-1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrchestratorAgent orchestrator;

    @Test
    void shouldAnswerWithTheFullResponseContract() throws Exception {
        when(orchestrator.answer(any(), any())).thenReturn(outcome());

        postQuery(REQUEST_BODY).andExpect(status().isOk())
               .andExpect(jsonPath("$.message").value(ANSWER))
               .andExpect(jsonPath("$.conversation_id").value(CONVERSATION_ID))
               .andExpect(jsonPath("$.query_interpretation.tools[0].name").value(SCHEDULE_TOOL))
               .andExpect(jsonPath("$.query_interpretation.tools[0].server").value(SCHEDULE_SERVER))
               .andExpect(jsonPath("$.query_interpretation.tools[0].origin").value("model"))
               .andExpect(jsonPath("$.query_interpretation.tools[0].duration_ms").value(42))
               .andExpect(jsonPath("$.meta.request_id").isNotEmpty())
               .andExpect(jsonPath("$.meta.model").value(MODEL))
               .andExpect(jsonPath("$.meta.prompt_tokens").value(1200))
               .andExpect(jsonPath("$.meta.rounds").value(2))
               .andExpect(jsonPath("$.meta.cache_hit").value(false))
               .andExpect(jsonPath("$.meta.refused").value(false));
    }

    @Test
    void shouldOmitWhatATraceDoesNotHave() throws Exception {
        when(orchestrator.answer(any(), any())).thenReturn(outcome());

        postQuery(REQUEST_BODY).andExpect(status().isOk())
               .andExpect(jsonPath("$.query_interpretation.tools[0].error").doesNotExist());
    }

    @Test
    void shouldCorrelateTheResponseWithTheRequestIdHeader() throws Exception {
        when(orchestrator.answer(any(), any())).thenReturn(outcome());

        var response = postQuery(REQUEST_BODY).andExpect(status().isOk())
                              .andExpect(header().exists(REQUEST_ID_HEADER))
                              .andReturn()
                              .getResponse();

        assertThat(response.getContentAsString()).contains(response.getHeader(REQUEST_ID_HEADER));
    }

    @Test
    void shouldReuseAnIncomingRequestId() throws Exception {
        when(orchestrator.answer(any(), any())).thenReturn(outcome());

        mockMvc.perform(post(QUERY_PATH).contentType(APPLICATION_JSON)
                                        .content(REQUEST_BODY)
                                        .header(REQUEST_ID_HEADER, CALLER_REQUEST_ID))
               .andExpect(status().isOk())
               .andExpect(header().string(REQUEST_ID_HEADER, CALLER_REQUEST_ID))
               .andExpect(jsonPath("$.meta.request_id").value(CALLER_REQUEST_ID));
    }

    @Test
    void shouldRejectABlankPrompt() throws Exception {
        postQuery(BLANK_PROMPT_BODY).andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message").isNotEmpty())
               .andExpect(jsonPath("$.query_interpretation.tools").isEmpty())
               .andExpect(jsonPath("$.conversation_id").isNotEmpty());
    }

    @Test
    void shouldRejectAPromptBeyondTheAllowedLength() throws Exception {
        var body = "{\"prompt\":\"" + "a".repeat(MAX_PROMPT_LENGTH + 1) + "\"}";
        postQuery(body).andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message").isNotEmpty())
               .andExpect(jsonPath("$.query_interpretation.tools").isEmpty());
        verifyNoInteractions(orchestrator);
    }

    @Test
    void shouldRejectAMalformedBody() throws Exception {
        postQuery(MALFORMED_BODY).andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message").isNotEmpty())
               .andExpect(jsonPath("$.query_interpretation.tools").isEmpty());
    }

    @Test
    void shouldReportAFailureWithoutLeakingDetails() throws Exception {
        when(orchestrator.answer(any(), any())).thenThrow(new IllegalStateException("boom"));

        postQuery(REQUEST_BODY).andExpect(status().isInternalServerError())
               .andExpect(jsonPath("$.message").value(not(containsString("boom"))))
               .andExpect(jsonPath("$.query_interpretation.tools").isEmpty());
    }

    private ResultActions postQuery(String body) throws Exception {
        return mockMvc.perform(post(QUERY_PATH).contentType(APPLICATION_JSON).content(body));
    }

    private QueryOutcome outcome() {
        return QueryOutcome.builder()
                           .message(ANSWER)
                           .conversationId(CONVERSATION_ID)
                           .calls(List.of(scheduleCall()))
                           .usage(TokenUsage.builder().model(MODEL).promptTokens(1200).completionTokens(80).build())
                           .costUsd(new BigDecimal("0.000608"))
                           .toolRounds(2)
                           .latencyMillis(1500)
                           .build();
    }

    private ToolCall scheduleCall() {
        return ToolCall.builder()
                       .name(SCHEDULE_TOOL)
                       .server(SCHEDULE_SERVER)
                       .origin(ToolOrigin.MODEL)
                       .arguments(Map.of())
                       .resultPreview("JDD 2026 AGENDA")
                       .durationMillis(42)
                       .build();
    }
}
