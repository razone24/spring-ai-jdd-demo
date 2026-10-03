package com.springai.jdd.websearch;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WebSearchToolsTest {

    private final TavilyClient client = mock(TavilyClient.class);
    private final TavilyProperties properties = new TavilyProperties();
    private final WebSearchTools tools = new WebSearchTools(client, properties, new ObjectMapper());

    @Test
    void reportsMissingApiKeyInsteadOfFailing() {
        String result = tools.searchWeb("spring ai", 3);

        assertThat(result).contains("not configured");
        verifyNoInteractions(client);
    }

    @Test
    void defaultsAndClampsTheResultCount() {
        properties.setApiKey("key");
        when(client.search(anyString(), anyInt())).thenReturn(List.of());

        tools.searchWeb("spring ai", null);
        tools.searchWeb("spring ai", 50);

        verify(client).search("spring ai", 5);
        verify(client).search("spring ai", 10);
    }
}
