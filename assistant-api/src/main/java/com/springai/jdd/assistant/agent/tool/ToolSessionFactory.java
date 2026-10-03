package com.springai.jdd.assistant.agent.tool;

import com.springai.jdd.assistant.agent.refusal.Refusal;
import com.springai.jdd.assistant.agent.refusal.RefusalTool;
import com.springai.jdd.assistant.agent.trail.ToolTrail;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ToolSessionFactory {

    static final String LOCAL_SERVER = "assistant-api";

    private final McpToolset toolset;
    private final ObjectMapper objectMapper;

    public ToolSession open() {
        ToolTrail trail = new ToolTrail();
        Refusal refusal = new Refusal();
        List<ToolCallback> tools = new ArrayList<>(toolset.modelTools(trail));
        for (ToolCallback local : ToolCallbacks.from(new RefusalTool(refusal))) {
            tools.add(new TracingToolCallback(local, LOCAL_SERVER, trail, objectMapper));
        }
        return new ToolSession(trail, refusal, tools);
    }
}
