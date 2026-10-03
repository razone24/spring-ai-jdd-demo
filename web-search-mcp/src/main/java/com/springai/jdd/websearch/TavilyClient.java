package com.springai.jdd.websearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class TavilyClient {

    public record SearchResult(String title, String url, String snippet, double score) {
    }

    private final RestTemplate restTemplate;
    private final TavilyProperties props;

    public TavilyClient(RestTemplate restTemplate, TavilyProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    public List<SearchResult> search(String query, int maxResults) {
        HttpEntity<TavilyRequest> request = new HttpEntity<>(new TavilyRequest(query, maxResults, "basic", false),
                                                             headers());
        for (int attempt = 0; attempt <= 1; attempt++) {
            try {
                TavilyResponse response = restTemplate.postForObject(props.getBaseUrl() + "/search", request,
                                                                     TavilyResponse.class);
                if (response == null || response.results() == null) {
                    return List.of();
                }
                return response.results().stream()
                        .map(r -> new SearchResult(r.title(), r.url(), r.content(), r.score()))
                        .toList();
            } catch (HttpServerErrorException e) {
                if (attempt == 1) {
                    throw e;
                }
            }
        }
        return List.of();
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(props.getApiKey());
        return headers;
    }
}

record TavilyRequest(
        String query,
        @JsonProperty("max_results") int maxResults,
        @JsonProperty("search_depth") String searchDepth,
        @JsonProperty("include_answer") boolean includeAnswer) {
}

record TavilyRawResult(String title, String url, String content, double score) {
}

record TavilyResponse(List<TavilyRawResult> results) {
}
