package com.springai.jdd.conference.mcp;

import com.springai.jdd.conference.tools.OverviewTools;
import com.springai.jdd.conference.tools.ScheduleTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
public class McpConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(McpConfiguration.class);

    @Bean
    public List<ToolCallback> toolCallbacks(OverviewTools overviewTools, ScheduleTools scheduleTools) {
        List<ToolCallback> callbacks = new ArrayList<>();
        Collections.addAll(callbacks, ToolCallbacks.from(overviewTools));
        Collections.addAll(callbacks, ToolCallbacks.from(scheduleTools));
        logger.info("Registered {} MCP tools", callbacks.size());
        return callbacks;
    }
}
