package com.springai.jdd.assistant.mcp;

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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.joining;

/**
 * Every tool the agent reaches over MCP — one client per server in {@code application.yml} — as a
 * Spring AI {@link ToolCallback}, tagged with the server it lives on. The model gets the tools; the
 * semantic-cache tools are reserved for the harness, which calls them itself.
 */
@Slf4j
@Component
public class McpToolset {

    public static final Set<String> HARNESS_ONLY = Set.of("searchChatHistory", "recordChatHistory");

    private final List<McpSyncClient> clients;
    private final ObjectMapper objectMapper;
    private volatile Map<String, ServerTool> tools = Map.of();

    McpToolset(List<McpSyncClient> clients, ObjectMapper objectMapper) {
        this.clients = List.copyOf(clients);
        this.objectMapper = objectMapper;
    }

    /**
     * The tools the model may call this turn, each traced into the turn's trail — sorted, so the
     * prompt (and with a fixed seed, the model's choices) doesn't depend on discovery order.
     */
    public List<ToolCallback> toolsFor(ToolTrail trail) {
        return discover().values()
                         .stream()
                         .filter(tool -> !HARNESS_ONLY.contains(tool.name()))
                         .sorted(Comparator.comparing(ServerTool::name))
                         .map(tool -> traced(tool.callback(), tool.server(), trail))
                         .toList();
    }

    public ToolCallback traced(ToolCallback tool, String server, ToolTrail trail) {
        return new TracingToolCallback(tool, server, trail, objectMapper);
    }

    /** A tool call the harness makes itself (not the model), recorded in the trail as such. */
    public String callAsHarness(String toolName, Map<String, Object> arguments, ToolTrail trail) {
        ServerTool tool = discover().get(toolName);
        if (tool == null) {
            throw new IllegalStateException("No MCP server offers the tool " + toolName);
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
            throw exception;
        }
    }

    /**
     * Lists the tools of every server, lazily. While a server is missing (down at start-up, say) the
     * next turn asks again, so it rejoins without restarting the agent.
     */
    private Map<String, ServerTool> discover() {
        Map<String, ServerTool> known = tools;
        if (!known.isEmpty() && serversIn(known) == clients.size()) {
            return known;
        }
        synchronized (this) {
            Map<String, ServerTool> found = new LinkedHashMap<>();
            clients.forEach(client -> discoverOn(client, found));
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
            log.warn("MCP server {} is unavailable, its tools are skipped: {}", client.getClientInfo().name(),
                     exception.getMessage());
        }
    }

    private static long serversIn(Map<String, ServerTool> known) {
        return known.values().stream().map(ServerTool::client).distinct().count();
    }

    private static String textOf(CallToolResult result) {
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
