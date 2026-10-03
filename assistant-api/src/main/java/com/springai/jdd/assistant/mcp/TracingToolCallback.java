package com.springai.jdd.assistant.mcp;

import com.springai.jdd.assistant.mcp.ToolCall;
import com.springai.jdd.assistant.mcp.ToolOrigin;
import com.springai.jdd.assistant.mcp.ToolTrail;
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
 * A failure reaches the model as a plain instruction rather than an exception class name, so it
 * says the information is unavailable instead of guessing.
 */
public final class TracingToolCallback implements ToolCallback {

    static final String TOOL_FAILED = "The tool %s on server %s is unavailable right now (%s). Tell the user this "
                                      + "information can't be looked up at the moment. Do not guess it.";

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
            String reason = rootCauseOf(exception);
            trail.record(callOf(toolInput, startedAt).error(reason).build());
            throw new ToolExecutionException(getToolDefinition(), new IllegalStateException(
                    TOOL_FAILED.formatted(getToolDefinition().name(), server, reason)));
        }
    }

    private static String rootCauseOf(Throwable exception) {
        Throwable root = exception;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
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
