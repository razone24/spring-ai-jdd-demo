package com.springai.jdd.assistant.agent.tool;

import com.springai.jdd.assistant.agent.trail.ToolOrigin;
import com.springai.jdd.assistant.agent.trail.ToolTrail;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TracingToolCallbackTest {

    private static final String TOOL = "searchKnowledgeBase";
    private static final String SERVER = "rag-search-mcp";
    private static final String INPUT = "{\"query\":\"wifi\"}";

    private final ToolTrail trail = new ToolTrail();

    @Test
    void shouldRecordTheCallWithTheTextInsideTheMcpEnvelope() {
        String envelope = "[{\"type\":\"text\",\"text\":\"Network JDD2026\"}]";

        String output = traced(input -> envelope).call(INPUT);

        assertThat(output).isEqualTo(envelope);
        assertThat(trail.calls()).singleElement().satisfies(call -> {
            assertThat(call.name()).isEqualTo(TOOL);
            assertThat(call.server()).isEqualTo(SERVER);
            assertThat(call.origin()).isEqualTo(ToolOrigin.MODEL);
            assertThat(call.arguments()).isEqualTo(Map.of("query", "wifi"));
            assertThat(call.resultPreview()).isEqualTo("Network JDD2026");
            assertThat(call.error()).isNull();
        });
    }

    @Test
    void shouldUnquoteAJsonEncodedTextResult() {
        traced(input -> "\"Line one\\nLine two\"").call(INPUT);

        assertThat(trail.calls().getFirst().resultPreview()).isEqualTo("Line one\nLine two");
    }

    @Test
    void shouldUnwrapAStringTheServerEncodedInsideTheEnvelope() {
        traced(input -> "[{\"type\":\"text\",\"text\":\"\\\"Agenda\\\"\"}]").call(INPUT);

        assertThat(trail.calls().getFirst().resultPreview()).isEqualTo("Agenda");
    }

    @Test
    void shouldTruncateLongResults() {
        traced(input -> "x".repeat(5_000)).call(INPUT);

        assertThat(trail.calls().getFirst().resultPreview()).hasSize(ToolResults.PREVIEW_LIMIT + 1);
    }

    @Test
    void shouldRecordAFailureAndHandItToTheToolLoopAsAToolError() {
        ToolCallback failing = traced(input -> {
            throw new IllegalStateException("server down");
        });

        assertThatThrownBy(() -> failing.call(INPUT)).isInstanceOf(ToolExecutionException.class);
        assertThat(trail.calls()).singleElement().satisfies(call -> assertThat(call.error()).isEqualTo("server down"));
    }

    private ToolCallback traced(Function<String, String> behaviour) {
        ToolDefinition definition = ToolDefinition.builder()
                                                  .name(TOOL)
                                                  .description("wiki")
                                                  .inputSchema("{}")
                                                  .build();
        ToolCallback delegate = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return definition;
            }

            @Override
            public String call(String toolInput) {
                return behaviour.apply(toolInput);
            }
        };
        return new TracingToolCallback(delegate, SERVER, trail, new ObjectMapper());
    }
}
