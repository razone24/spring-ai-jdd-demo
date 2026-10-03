package com.springai.jdd.websearch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.springai.jdd.websearch.TavilyClient.SearchResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TavilyClientTest {

    private MockRestServiceServer mockServer;
    private TavilyClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        TavilyProperties props = new TavilyProperties();
        props.setApiKey("test-key");
        props.setBaseUrl("https://api.tavily.com");
        props.setTimeoutSeconds(10);
        client = new TavilyClient(restTemplate, props);
    }

    @Test
    void mapsResponseToResults() {
        String json = """
                {
                  "results": [
                    {"title": "JDD 2026", "url": "https://jdd.org.pl", "content": "Java conference", "score": 0.95}
                  ]
                }
                """;
        mockServer.expect(once(), requestTo("https://api.tavily.com/search"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<SearchResult> results = client.search("java conference", 3);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("JDD 2026");
        assertThat(results.get(0).url()).isEqualTo("https://jdd.org.pl");
        assertThat(results.get(0).snippet()).isEqualTo("Java conference");
        assertThat(results.get(0).score()).isEqualTo(0.95);
        mockServer.verify();
    }

    @Test
    void retriesOnceOn5xx() {
        String json = """
                {"results": [{"title": "T", "url": "u", "content": "c", "score": 0.5}]}
                """;
        mockServer.expect(once(), requestTo("https://api.tavily.com/search"))
                .andRespond(withServerError());
        mockServer.expect(once(), requestTo("https://api.tavily.com/search"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<SearchResult> results = client.search("query", 1);

        assertThat(results).hasSize(1);
        mockServer.verify();
    }
}