package com.springai.jdd.llm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * Checks that the selected LLM provider is usable before any model bean is created: the provider is known, OpenAI has a
 * real-looking key, and (unless {@code llm.verify-connectivity=false}) the provider answers one cheap request.
 */
public class LlmProviderCheck implements AutoCloseable {

    private static final Log log = LogFactory.getLog(LlmProviderCheck.class);
    private static final Set<String> PLACEHOLDER_KEYS = Set.of("not-set", "sk-...", "changeme");
    private static final String DEFAULT_OPENAI_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";

    private final HttpClient http;
    private final Duration openAiTimeout;
    private final Duration ollamaTimeout;

    public LlmProviderCheck(HttpClient http, Duration openAiTimeout, Duration ollamaTimeout) {
        this.http = http;
        this.openAiTimeout = openAiTimeout;
        this.ollamaTimeout = ollamaTimeout;
    }

    public static LlmProviderCheck withDefaults() {
        Duration ollamaTimeout = Duration.ofSeconds(2);
        return new LlmProviderCheck(HttpClient.newBuilder().connectTimeout(ollamaTimeout).build(),
                                    Duration.ofSeconds(5), ollamaTimeout);
    }

    public void verify(Environment env) {
        LlmProvider provider = LlmProvider.parse(env.getProperty("llm.provider"));
        boolean verifyConnectivity = env.getProperty("llm.verify-connectivity", Boolean.class, true);
        String ollamaUrl = baseUrl(env, "spring.ai.ollama.base-url", DEFAULT_OLLAMA_URL);
        switch (provider) {
            case OPENAI -> verifyOpenAi(env, ollamaUrl, verifyConnectivity);
            case OLLAMA -> {
                if (verifyConnectivity) {
                    verifyOllama(ollamaUrl);
                }
            }
        }
    }

    private void verifyOpenAi(Environment env, String ollamaUrl, boolean verifyConnectivity) {
        String key = env.getProperty("spring.ai.openai.api-key", "").strip();
        if (key.isEmpty() || PLACEHOLDER_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
            throw new LlmProviderNotAvailableException(
                    "LLM provider 'openai' is selected but OPENAI_API_KEY is not set.",
                    "Set OPENAI_API_KEY, or run with LLM_PROVIDER=ollama to use a local Ollama at " + ollamaUrl + ".");
        }
        if (!verifyConnectivity) {
            return;
        }
        String url = baseUrl(env, "spring.ai.openai.base-url", DEFAULT_OPENAI_URL);
        String modelsUrl = url + "/models";
        HttpRequest request = HttpRequest.newBuilder(URI.create(modelsUrl))
                                         .timeout(openAiTimeout)
                                         .header("Authorization", "Bearer " + key)
                                         .GET()
                                         .build();
        int status;
        try {
            status = send(request);
        } catch (IOException e) {
            throw new LlmProviderNotAvailableException("OpenAI is not reachable at " + url + ".",
                    "Check your network and OPENAI_BASE_URL, or run with LLM_PROVIDER=ollama.");
        }
        if (status == 401 || status == 403) {
            throw new LlmProviderNotAvailableException(
                    "OpenAI rejected the configured OPENAI_API_KEY (HTTP " + status + " from " + modelsUrl + ").",
                    "Check OPENAI_API_KEY, or run with LLM_PROVIDER=ollama.");
        }
        if (status == 404 || status == 429 || status >= 500) {
            log.warn("Could not verify OPENAI_API_KEY: " + modelsUrl + " returned " + status + "; continuing.");
            return;
        }
        if (status < 200 || status >= 300) {
            throw new LlmProviderNotAvailableException("OpenAI returned HTTP " + status + " from " + modelsUrl + ".",
                    "Check OPENAI_BASE_URL and the OpenAI status page, or run with LLM_PROVIDER=ollama.");
        }
    }

    private void verifyOllama(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url + "/api/tags")).timeout(ollamaTimeout).GET().build();
        int status;
        try {
            status = send(request);
        } catch (IOException e) {
            status = -1;
        }
        if (status < 200 || status >= 300) {
            throw new LlmProviderNotAvailableException("Ollama is not reachable at " + url + ".",
                    "Start Ollama (ollama serve) or set OLLAMA_BASE_URL, or run with LLM_PROVIDER=openai.");
        }
    }

    private int send(HttpRequest request) throws IOException {
        try {
            return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while checking the LLM provider", e);
        }
    }

    @Override
    public void close() {
        http.close();
    }

    private static String baseUrl(Environment env, String property, String fallback) {
        String url = env.getProperty(property, fallback).strip();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
