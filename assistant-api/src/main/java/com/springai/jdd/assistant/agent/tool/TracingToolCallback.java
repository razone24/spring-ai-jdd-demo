package com.springai.jdd.assistant.agent.tool;

import com.springai.jdd.assistant.agent.trail.ToolCall;
import com.springai.jdd.assistant.agent.trail.ToolOrigin;
import com.springai.jdd.assistant.agent.trail.ToolTrail;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.metadata.ToolMetadata;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * Decorates a tool so every call the model makes lands in the request's {@link ToolTrail}
 * with its server, arguments, result preview, duration and error — without the tool knowing.
 */
public final class TracingToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final String server;
    private final ToolTrail trail;
    private final ObjectMapper objectMapper;

    public TracingToolCallback(ToolCallback delegate, String server, ToolTrail trail, ObjectMapper objectMapper) {
        this.delegate = delegate;
        this.server = server;
        this.trail = trail;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        long startedAt = System.nanoTime();
        try {
            String output = toolContext == null ? delegate.call(toolInput) : delegate.call(toolInput, toolContext);
            trail.record(callOf(toolInput, startedAt).resultPreview(ToolResults.previewOf(output, objectMapper))
                                                     .build());
            return output;
        } catch (RuntimeException exception) {
            trail.record(callOf(toolInput, startedAt).error(exception.getMessage()).build());
            throw exception instanceof ToolExecutionException ? exception
                                                              : new ToolExecutionException(getToolDefinition(), exception);
        }
    }

    private ToolCall.ToolCallBuilder callOf(String toolInput, long startedAt) {
        return ToolCall.builder()
                       .name(getToolDefinition().name())
                       .server(server)
                       .origin(ToolOrigin.MODEL)
                       .arguments(ToolResults.argumentsOf(toolInput, objectMapper))
                       .durationMillis(Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
    }
}
