package com.springai.jdd.assistant.agent.tool;

import com.springai.jdd.assistant.agent.trail.ToolCall;
import com.springai.jdd.assistant.agent.trail.ToolOrigin;
import com.springai.jdd.assistant.agent.trail.ToolTrail;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.joining;

/**
 * Every tool the agent can reach over MCP, indexed by tool name and tagged with the server that
 * serves it. The model gets most of them; the semantic-cache tools are reserved for the harness.
 */
@Slf4j
@Component
public class McpToolset {

    public static final Set<String> HARNESS_ONLY = Set.of("searchChatHistory", "recordChatHistory");
    private static final String UNKNOWN_TOOL = "No MCP server offers the tool %s";
    private static final String SERVER_UNAVAILABLE = "MCP server {} is unavailable, its tools are skipped: {}";

    private final List<McpSyncClient> clients;
    private final ObjectMapper objectMapper;
    private static final long REDISCOVERY_INTERVAL_NANOS = Duration.ofSeconds(10).toNanos();

    private volatile Map<String, ServerTool> tools = Map.of();
    private volatile long discoveredAt;

    McpToolset(List<McpSyncClient> clients, ObjectMapper objectMapper) {
        this.clients = List.copyOf(clients);
        this.objectMapper = objectMapper;
    }

    public List<ToolCallback> modelTools(ToolTrail trail) {
        return discover().values()
                         .stream()
                         .filter(tool -> !HARNESS_ONLY.contains(tool.name()))
                         .map(tool -> (ToolCallback) new TracingToolCallback(tool.callback(), tool.server(), trail,
                                                                             objectMapper))
                         .toList();
    }

    /**
     * Calls a tool on behalf of the harness (not the model) and records it in the trail as such.
     */
    public String callAsHarness(String toolName, Map<String, Object> arguments, ToolTrail trail) {
        ServerTool tool = discover().get(toolName);
        if (tool == null) {
            throw new IllegalStateException(UNKNOWN_TOOL.formatted(toolName));
        }
        long startedAt = System.nanoTime();
        ToolCall.ToolCallBuilder call = ToolCall.builder()
                                                .name(toolName)
                                                .server(tool.server())
                                                .origin(ToolOrigin.HARNESS)
                                                .arguments(arguments);
        try {
            CallToolResult result = tool.client().callTool(new CallToolRequest(toolName, arguments));
            String text = textOf(result);
            if (Boolean.TRUE.equals(result.isError())) {
                throw new IllegalStateException(text);
            }
            trail.record(call.resultPreview(ToolResults.truncate(text)).durationMillis(elapsed(startedAt)).build());
            return text;
        } catch (RuntimeException exception) {
            trail.record(call.error(exception.getMessage()).durationMillis(elapsed(startedAt)).build());
            invalidate();
            throw exception;
        }
    }

    /**
     * Tool lists are discovered lazily and re-discovered after a failure, so a server that was
     * down at start-up (or restarted) rejoins without restarting the agent.
     */
    private Map<String, ServerTool> discover() {
        Map<String, ServerTool> known = tools;
        if (!known.isEmpty() && (!anyServerMissing(known) || !rediscoveryDue())) {
            return known;
        }
        synchronized (this) {
            discoveredAt = System.nanoTime();
            Map<String, ServerTool> found = new LinkedHashMap<>();
            for (McpSyncClient client : clients) {
                discoverOn(client, found);
            }
            tools = Map.copyOf(found);
            return tools;
        }
    }

    private void discoverOn(McpSyncClient client, Map<String, ServerTool> found) {
        try {
            if (!client.isInitialized()) {
                client.initialize();
            }
            String server = client.getServerInfo().name();
            for (Tool tool : client.listTools().tools()) {
                ToolCallback callback = SyncMcpToolCallback.builder()
                                                           .mcpClient(client)
                                                           .tool(tool)
                                                           .prefixedToolName(tool.name())
                                                           .build();
                found.put(tool.name(), new ServerTool(tool.name(), server, client, callback));
            }
        } catch (RuntimeException exception) {
            log.warn(SERVER_UNAVAILABLE, client.getClientInfo().name(), exception.getMessage());
        }
    }

    private boolean anyServerMissing(Map<String, ServerTool> known) {
        return known.values().stream().map(ServerTool::client).distinct().count() < clients.size();
    }

    private boolean rediscoveryDue() {
        return System.nanoTime() - discoveredAt > REDISCOVERY_INTERVAL_NANOS;
    }

    private void invalidate() {
        tools = Map.of();
    }

    private String textOf(CallToolResult result) {
        return result.content()
                     .stream()
                     .filter(TextContent.class::isInstance)
                     .map(content -> ((TextContent) content).text())
                     .collect(joining());
    }

    private static long elapsed(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private record ServerTool(String name, String server, McpSyncClient client, ToolCallback callback) {
    }
}
