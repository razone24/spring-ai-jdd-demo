package com.springai.jdd.websearch;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

@Component
public class WebSearchTools {

    static final String NOT_CONFIGURED = "Web search is not configured on this server (no TAVILY_API_KEY). "
                                         + "Answer from the other tools, or tell the user the web is unavailable.";
    private static final int DEFAULT_RESULTS = 5;
    private static final int MAX_RESULTS = 10;

    private final TavilyClient tavilyClient;
    private final TavilyProperties properties;
    private final ObjectMapper objectMapper;

    public WebSearchTools(TavilyClient tavilyClient, TavilyProperties properties, ObjectMapper objectMapper) {
        this.tavilyClient = tavilyClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Tool(name = "searchWeb",
          description = """
                  Search the public internet for current, real-time, or general information that is not about \
                  the JDD conference itself (e.g. latest library releases, news, technical facts). \
                  Returns web results with titles, URLs and content snippets.""")
    public String searchWeb(@ToolParam(description = "The search text") String query,
                            @ToolParam(description = "How many results to return (1-10, default 5)", required = false)
                            Integer maxResults) {
        if (!hasText(properties.getApiKey())) {
            return objectMapper.writeValueAsString(Map.of("error", NOT_CONFIGURED));
        }
        int limit = maxResults == null ? DEFAULT_RESULTS : Math.clamp(maxResults, 1, MAX_RESULTS);
        return objectMapper.writeValueAsString(tavilyClient.search(query, limit));
    }
}
