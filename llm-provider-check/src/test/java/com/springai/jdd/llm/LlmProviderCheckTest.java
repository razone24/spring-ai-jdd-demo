package com.springai.jdd.llm;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class LlmProviderCheckTest {

    private static final String PROVIDER_ACTION = "Set LLM_PROVIDER to 'openai' (default) or 'ollama', or unset it.";

    private final LlmProviderCheck check = new LlmProviderCheck(
            HttpClient.newBuilder().connectTimeout(Duration.ofMillis(200)).build(),
            Duration.ofMillis(300), Duration.ofMillis(300));

    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldRejectEmptyProvider() {
        LlmProviderNotAvailableException failure = failureOf(env().withProperty("llm.provider", ""));

        assertThat(failure.description()).isEqualTo("LLM_PROVIDER is set but empty.");
        assertThat(failure.action()).isEqualTo(PROVIDER_ACTION);
    }

    @Test
    void shouldRejectUnknownProvider() {
        LlmProviderNotAvailableException failure = failureOf(env().withProperty("llm.provider", "anthropic"));

        assertThat(failure.description()).isEqualTo("LLM_PROVIDER is 'anthropic', which is not a supported LLM provider (use lowercase).");
        assertThat(failure.action()).isEqualTo(PROVIDER_ACTION);
    }

    @Test
    void shouldRejectMixedCaseProvider() {
        // The YAML builds table and property names from the raw value, so 'OpenAI' would fail later, buried.
        LlmProviderNotAvailableException failure = failureOf(env().withProperty("llm.provider", "OpenAI"));

        assertThat(failure.description())
                .isEqualTo("LLM_PROVIDER is 'OpenAI', which is not a supported LLM provider (use lowercase).");
        assertThat(failure.action()).isEqualTo(PROVIDER_ACTION);
    }

    @Test
    void shouldDefaultToOpenAiWhenProviderMissing() {
        LlmProviderNotAvailableException failure = failureOf(env());

        assertThat(failure.description()).isEqualTo("LLM provider 'openai' is selected but OPENAI_API_KEY is not set.");
        assertThat(failure.action()).isEqualTo(
                "Set OPENAI_API_KEY, or run with LLM_PROVIDER=ollama to use a local Ollama at http://localhost:11434.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "not-set", "sk-...", "CHANGEME"})
    void shouldRejectPlaceholderKey(String key) {
        LlmProviderNotAvailableException failure = failureOf(env().withProperty("spring.ai.openai.api-key", key));

        assertThat(failure.description()).isEqualTo("LLM provider 'openai' is selected but OPENAI_API_KEY is not set.");
    }

    @Test
    void shouldRejectKeyEvenWhenConnectivityCheckIsOff() {
        LlmProviderNotAvailableException failure = failureOf(env()
                .withProperty("llm.verify-connectivity", "false")
                .withProperty("spring.ai.openai.api-key", "not-set"));

        assertThat(failure.description()).isEqualTo("LLM provider 'openai' is selected but OPENAI_API_KEY is not set.");
    }

    @Test
    void shouldPassWhenOpenAiAcceptsKey() throws IOException {
        String url = stub(200);

        assertThatCode(() -> check.verify(openAi(url + "/v1", "sk-test"))).doesNotThrowAnyException();
        assertThat(requestPath.get()).isEqualTo("/v1/models");
        assertThat(authorization.get()).isEqualTo("Bearer sk-test");
    }

    @Test
    void shouldTolerateTrailingSlashInBaseUrl() throws IOException {
        String url = stub(200);

        check.verify(openAi(url + "/v1/", "sk-test"));

        assertThat(requestPath.get()).isEqualTo("/v1/models");
    }

    @Test
    void shouldRejectKeyOpenAiRefuses() throws IOException {
        String url = stub(401) + "/v1";

        LlmProviderNotAvailableException failure = failureOf(openAi(url, "sk-secret-123"));

        assertThat(failure.description())
                .isEqualTo("OpenAI rejected the configured OPENAI_API_KEY (HTTP 401 from " + url + "/models).");
        assertThat(failure.action()).isEqualTo("Check OPENAI_API_KEY, or run with LLM_PROVIDER=ollama.");
        assertThat(failure.description() + failure.action()).doesNotContain("sk-secret-123");
    }

    @Test
    void shouldContinueWhenModelsEndpointMissing() throws IOException {
        String url = stub(404);

        assertThatCode(() -> check.verify(openAi(url, "sk-test"))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 500, 503})
    void shouldContinueWhenOpenAiIsTemporarilyUnavailable(int status) throws IOException {
        // A rate limit or an outage at startup is not a configuration error; the first real call will retry.
        String url = stub(status);

        assertThatCode(() -> check.verify(openAi(url, "sk-test"))).doesNotThrowAnyException();
    }

    @Test
    void shouldFailWhenOpenAiRejectsTheRequest() throws IOException {
        String url = stub(400);

        LlmProviderNotAvailableException failure = failureOf(openAi(url, "sk-test"));

        assertThat(failure.description()).isEqualTo("OpenAI returned HTTP 400 from " + url + "/models.");
        assertThat(failure.action())
                .isEqualTo("Check OPENAI_BASE_URL and the OpenAI status page, or run with LLM_PROVIDER=ollama.");
    }

    @Test
    void shouldFailWhenOpenAiUnreachable() throws IOException {
        String url = closedPortUrl();

        LlmProviderNotAvailableException failure = failureOf(openAi(url, "sk-test"));

        assertThat(failure.description()).isEqualTo("OpenAI is not reachable at " + url + ".");
        assertThat(failure.action())
                .isEqualTo("Check your network and OPENAI_BASE_URL, or run with LLM_PROVIDER=ollama.");
    }

    @Test
    void shouldPassWhenOllamaAnswers() throws IOException {
        String url = stub(200);

        assertThatCode(() -> check.verify(ollama(url))).doesNotThrowAnyException();
        assertThat(requestPath.get()).isEqualTo("/api/tags");
    }

    @Test
    void shouldFailWhenOllamaUnreachable() throws IOException {
        String url = closedPortUrl();

        LlmProviderNotAvailableException failure = failureOf(ollama(url));

        assertThat(failure.description()).isEqualTo("Ollama is not reachable at " + url + ".");
        assertThat(failure.action())
                .isEqualTo("Start Ollama (ollama serve) or set OLLAMA_BASE_URL, or run with LLM_PROVIDER=openai.");
    }

    @Test
    void shouldFailFastWhenOllamaHangs() throws IOException {
        String url = stub(Duration.ofSeconds(5));

        LlmProviderNotAvailableException failure =
                assertTimeoutPreemptively(Duration.ofSeconds(2), () -> failureOf(ollama(url)));

        assertThat(failure.description()).isEqualTo("Ollama is not reachable at " + url + ".");
    }

    @Test
    void shouldSkipNetworkWhenConnectivityCheckOff() throws IOException {
        MockEnvironment env = openAi(closedPortUrl(), "sk-test").withProperty("llm.verify-connectivity", "false");

        assertThatCode(() -> check.verify(env)).doesNotThrowAnyException();
    }

    @Test
    void shouldReleaseTheHttpClientWhenClosed() {
        HttpClient client = HttpClient.newHttpClient();

        new LlmProviderCheck(client, Duration.ofMillis(300), Duration.ofMillis(300)).close();

        assertThat(client.isTerminated()).isTrue();
    }

    private LlmProviderNotAvailableException failureOf(MockEnvironment env) {
        LlmProviderNotAvailableException failure =
                catchThrowableOfType(LlmProviderNotAvailableException.class, () -> check.verify(env));
        assertThat(failure).as("expected the check to fail").isNotNull();
        return failure;
    }

    private static MockEnvironment env() {
        return new MockEnvironment();
    }

    private static MockEnvironment openAi(String baseUrl, String key) {
        return env().withProperty("spring.ai.openai.base-url", baseUrl).withProperty("spring.ai.openai.api-key", key);
    }

    private static MockEnvironment ollama(String baseUrl) {
        return env().withProperty("llm.provider", "ollama").withProperty("spring.ai.ollama.base-url", baseUrl);
    }

    private String stub(int status) throws IOException {
        return startStub(status, Duration.ZERO);
    }

    private String stub(Duration delay) throws IOException {
        return startStub(200, delay);
    }

    private String startStub(int status, Duration delay) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static String closedPortUrl() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return "http://127.0.0.1:" + socket.getLocalPort();
        }
    }
}
