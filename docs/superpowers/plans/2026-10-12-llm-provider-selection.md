# LLM Provider Selection and Fail-Fast Startup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** OpenAI by default and Ollama on request, chosen by one `LLM_PROVIDER` switch across all AI modules, with a clear Spring Boot failure report instead of a stack trace when the chosen provider isn't usable.

**Architecture:** A new plain-jar module `llm-provider-check` contributes an auto-configured `BeanFactoryPostProcessor` that validates the provider (and optionally probes it over HTTP) before any Spring AI model bean is created, throwing an exception that a registered `FailureAnalyzer` renders as Description/Action. Provider-dependent values (model selection, embedding model, table names, thresholds) are resolved in YAML from the single `llm.provider` property (env `LLM_PROVIDER`, default `openai`) using nested placeholders, so almost no Java changes outside the new module.

**Tech Stack:** Java 25, Spring Boot 4.1.1, Spring AI 2.0.1 (BOM), pgvector, JUnit 5 / AssertJ / Mockito, JDK `HttpClient` + `com.sun.net.httpserver.HttpServer` for test stubs.

**Spec:** `docs/superpowers/specs/2026-10-12-llm-provider-selection-design.md` (all paths below are relative to `spring-ai-jdd-demo/`, the git repo root; branch `feature/add_multiple_llms_support`).

## Global Constraints

- `LLM_PROVIDER` values: `openai` | `ollama`, default `openai`. In YAML always reference it as `${llm.provider:openai}` (Spring's env lookup maps `LLM_PROVIDER` → `llm.provider`); never define `llm.provider` as a YAML key.
- OpenAI chat `${OPENAI_MODEL:gpt-4o-mini}`; OpenAI embedding `${OPENAI_EMBEDDING_MODEL:text-embedding-3-small}` with `dimensions: 768`.
- Ollama chat `${OLLAMA_MODEL:qwen2.5:7b-instruct}`; Ollama embedding `${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}`. `EMBEDDING_MODEL` is dropped.
- pgvector `dimensions` stays `${EMBEDDING_DIMENSIONS:768}` for both providers.
- Connectivity check switch: `llm.verify-connectivity` (env `LLM_VERIFY_CONNECTIVITY`), default `true`. (Renamed from the spec's `llm.provider.verify-connectivity`: `llm.provider` is a scalar and can't also be a prefix.)
- Timeouts: OpenAI probe 5 s, Ollama probe 2 s.
- Failure messages never contain the API key value.
- Thresholds: chat-history floor openai `0.4` / ollama `0.5`; cache hit openai `0.8` / ollama `0.9`. Env overrides `CHAT_HISTORY_SIMILARITY_THRESHOLD`, `ASSISTANT_CACHE_HIT_THRESHOLD` still win. OpenAI values carry a `# guess — tune on real data` comment.
- In every module with the OpenAI starter, set `spring.ai.model.image`, `spring.ai.model.audio.speech`, `spring.ai.model.audio.transcription`, `spring.ai.model.moderation` to `none` (otherwise those OpenAI beans demand a key even when `LLM_PROVIDER=ollama`).
- Shell commands go through `rtk` (repo CLAUDE.md). Commit messages end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **`LLM_PROVIDER` present but empty** (compose's `${LLM_PROVIDER:-}`, a blank `.env` line) → "LLM_PROVIDER is set but empty", not "unknown provider ''". Test: Task 1 `shouldRejectEmptyProvider`.
2. **Ollama accepts the TCP connection but never answers** (stuck/loading) → fails within the probe timeout with the "not reachable" message, never hangs startup. Test: Task 1 `shouldFailFastWhenOllamaHangs`.
3. **`OPENAI_BASE_URL` with a trailing slash** (`https://api.openai.com/v1/`) → probes `/v1/models`, not `/v1//models`. Test: Task 1 `shouldTolerateTrailingSlashInBaseUrl`.
4. **Compose always passing a threshold env var** would silently override per-provider defaults (today it passes `ASSISTANT_CACHE_HIT_THRESHOLD:-0.9`). Thresholds must be pass-through-only-if-set. Test: Task 5 step "compose config check".
5. **Rejected key echoed into logs** → description must not contain the key. Test: Task 1 `shouldRejectKeyOpenAiRefuses` asserts the key is absent from description and action.

---

### Task 0: Commit the spec

- [ ] **Step 1:** `rtk git add docs/superpowers/specs docs/superpowers/plans && rtk git commit -m "docs: design and plan for LLM provider selection"` (with the Co-Authored-By trailer).

---

### Task 1: `llm-provider-check` module

**Files:**
- Modify: `pom.xml` (add `<module>llm-provider-check</module>` **first** in `<modules>`)
- Create: `llm-provider-check/pom.xml`
- Create: `llm-provider-check/src/main/java/com/springai/jdd/llm/LlmProvider.java`
- Create: `llm-provider-check/src/main/java/com/springai/jdd/llm/LlmProviderNotAvailableException.java`
- Create: `llm-provider-check/src/main/java/com/springai/jdd/llm/LlmProviderNotAvailableFailureAnalyzer.java`
- Create: `llm-provider-check/src/main/java/com/springai/jdd/llm/LlmProviderCheck.java`
- Create: `llm-provider-check/src/main/java/com/springai/jdd/llm/LlmProviderCheckPostProcessor.java`
- Create: `llm-provider-check/src/main/java/com/springai/jdd/llm/LlmProviderCheckAutoConfiguration.java`
- Create: `llm-provider-check/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `llm-provider-check/src/main/resources/META-INF/spring.factories` (key `org.springframework.boot.diagnostics.FailureAnalyzer`)
- Modify: all five `*/Dockerfile`: add `COPY llm-provider-check/pom.xml llm-provider-check/` next to the other pom copies (the reactor needs every module pom); in `rag-search-mcp`, `chat-history-mcp` and `assistant-api` also add `COPY llm-provider-check/src/ llm-provider-check/src/` before the `package` step, so Docker builds keep working once Tasks 2–4 add the dependency.
- Test: `llm-provider-check/src/test/java/com/springai/jdd/llm/LlmProviderCheckTest.java`
- Test: `llm-provider-check/src/test/java/com/springai/jdd/llm/LlmProviderCheckAutoConfigurationTest.java`

**pom:** parent `jdd-demo-parent`, artifact `llm-provider-check`, packaging `jar`, **no** `spring-boot-maven-plugin`. Dependencies: `org.springframework.boot:spring-boot-autoconfigure`; test: `spring-boot-starter-test`.

**Interfaces (Produces):**
```java
public enum LlmProvider { OPENAI, OLLAMA;
    public String id();                        // "openai" / "ollama"
    public static LlmProvider parse(String raw); // null → OPENAI; blank → throws; case-insensitive; else throws
}
public class LlmProviderNotAvailableException extends RuntimeException {
    public LlmProviderNotAvailableException(String description, String action);
    public String description(); public String action();   // getMessage() == description
}
public class LlmProviderCheck {
    public LlmProviderCheck(HttpClient http, Duration openAiTimeout, Duration ollamaTimeout);
    public static LlmProviderCheck withDefaults();          // 5 s / 2 s, HttpClient with connectTimeout = the shorter
    public void verify(Environment env);                     // throws LlmProviderNotAvailableException
}
```
Properties read by `verify`: `llm.provider`, `llm.verify-connectivity` (default `true`), `spring.ai.openai.api-key`, `spring.ai.openai.base-url` (default `https://api.openai.com/v1`), `spring.ai.ollama.base-url` (default `http://localhost:11434`).

**Exact copy** (`{url}` = the configured base URL with any trailing `/` removed; `{value}` = raw provider value in single quotes):

| Case | Description | Action |
|---|---|---|
| blank provider | `LLM_PROVIDER is set but empty.` | `Set LLM_PROVIDER to 'openai' (default) or 'ollama', or unset it.` |
| unknown provider | `LLM_PROVIDER is {value}, which is not a supported LLM provider.` | same as above |
| key missing/placeholder (`""`, blank, `not-set`, `sk-...`, `changeme`, case-insensitive) | `LLM provider 'openai' is selected but OPENAI_API_KEY is not set.` | `Set OPENAI_API_KEY, or run with LLM_PROVIDER=ollama to use a local Ollama at {ollamaUrl}.` |
| 401/403 | `OpenAI rejected the configured OPENAI_API_KEY (HTTP {status} from {url}/models).` | `Check OPENAI_API_KEY, or run with LLM_PROVIDER=ollama.` |
| other non-2xx, non-404 | `OpenAI returned HTTP {status} from {url}/models.` | `Check OPENAI_BASE_URL and the OpenAI status page, or run with LLM_PROVIDER=ollama.` |
| OpenAI connect fail/timeout | `OpenAI is not reachable at {url}.` | `Check your network and OPENAI_BASE_URL, or run with LLM_PROVIDER=ollama.` |
| Ollama connect fail/timeout/non-2xx | `Ollama is not reachable at {url}.` | `Start Ollama (ollama serve) or set OLLAMA_BASE_URL, or run with LLM_PROVIDER=openai.` |

404 from `/models` → log WARN `Could not verify OPENAI_API_KEY: {url}/models returned 404; continuing.` and pass. Probes: `GET {url}/models` with `Authorization: Bearer <key>`; `GET {url}/api/tags`. Placeholder/blank key check runs even when connectivity is off.

- [ ] **Step 1: Write failing tests** in `LlmProviderCheckTest` using `MockEnvironment` and a `HttpServer` stub on port 0 (helper `stub(int status)` / `stub(Duration delay)` that records request path and `Authorization` header). Construct the check with `new LlmProviderCheck(HttpClient.newBuilder().connectTimeout(Duration.ofMillis(200)).build(), Duration.ofMillis(300), Duration.ofMillis(300))`. Each test asserts on `description()`/`action()` with the exact copy above:
  - `shouldRejectEmptyProvider` — `llm.provider=""`.
  - `shouldRejectUnknownProvider` — `"anthropic"` → description contains `'anthropic'`.
  - `shouldAcceptProviderCaseInsensitively` — `"OpenAI"`, key `sk-test`, stub 200 → no exception.
  - `shouldDefaultToOpenAiWhenProviderMissing` — no provider, no key → key-missing copy.
  - `shouldRejectPlaceholderKey` — `@ParameterizedTest` over `""`, `"  "`, `"not-set"`, `"sk-..."`, `"CHANGEME"`.
  - `shouldRejectKeyEvenWhenConnectivityCheckIsOff` — `llm.verify-connectivity=false`, key `not-set`.
  - `shouldPassWhenOpenAiAcceptsKey` — 200; asserts path `/v1/models` and header `Bearer sk-test` (base-url `http://127.0.0.1:{port}/v1`).
  - `shouldTolerateTrailingSlashInBaseUrl` — base-url ends with `/`; asserts path `/v1/models`.
  - `shouldRejectKeyOpenAiRefuses` — 401 → rejected copy; `assertThat(description + action).doesNotContain("sk-secret-123")`.
  - `shouldContinueWhenModelsEndpointMissing` — 404 → no exception.
  - `shouldFailWhenOpenAiReturnsServerError` — 503 → contains `HTTP 503`.
  - `shouldFailWhenOpenAiUnreachable` — base-url on a closed port (bind a `ServerSocket(0)`, read port, close it).
  - `shouldPassWhenOllamaAnswers` — provider `ollama`, 200 on `/api/tags`.
  - `shouldFailWhenOllamaUnreachable` — closed port → Ollama copy with that URL.
  - `shouldFailFastWhenOllamaHangs` — stub sleeps 5 s; `assertTimeoutPreemptively(Duration.ofSeconds(2), ...)` expects the Ollama copy.
  - `shouldSkipNetworkWhenConnectivityCheckOff` — `llm.verify-connectivity=false`, key `sk-test`, closed port → no exception.

- [ ] **Step 2:** `rtk ./mvnw -q -pl llm-provider-check test` → fails to compile (types missing).

- [ ] **Step 3: Implement** `LlmProvider`, `LlmProviderNotAvailableException`, `LlmProviderCheck` per the interfaces and table. Use `HttpRequest.timeout(...)` for the per-provider timeout; treat `IOException`/`HttpTimeoutException` as "not reachable"; restore the interrupt flag on `InterruptedException` and treat it as not reachable.

- [ ] **Step 4:** `rtk ./mvnw -q -pl llm-provider-check test` → all `LlmProviderCheckTest` pass.

- [ ] **Step 5: Write failing tests** in `LlmProviderCheckAutoConfigurationTest`:
  - `shouldStopContextBeforeBeansWhenProviderInvalid` — `new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(LlmProviderCheckAutoConfiguration.class)).withPropertyValues("llm.provider=bogus").withBean(ExplodingBean.class)` where `ExplodingBean`'s constructor throws `IllegalStateException("model bean created")`; assert `context.getStartupFailure()` has root cause `LlmProviderNotAvailableException` (proves the check runs before singletons).
  - `shouldStartWhenProviderValid` — `llm.provider=ollama`, `llm.verify-connectivity=false` → `hasNotFailed()`.
  - `shouldRenderDescriptionAndAction` — `new LlmProviderNotAvailableFailureAnalyzer().analyze(new RuntimeException(new LlmProviderNotAvailableException("d","a")))` → description `d`, action `a`, cause is the inner exception.
  - `shouldRegisterFailureAnalyzer` — `SpringFactoriesLoader.forDefaultResourceLocation().load(FailureAnalyzer.class)` contains an `LlmProviderNotAvailableFailureAnalyzer`.

- [ ] **Step 6:** run → fails.

- [ ] **Step 7: Implement** `LlmProviderNotAvailableFailureAnalyzer extends AbstractFailureAnalyzer<LlmProviderNotAvailableException>` (returns `new FailureAnalysis(cause.description(), cause.action(), cause)`); `LlmProviderCheckPostProcessor implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered` (order `HIGHEST_PRECEDENCE`, calls `LlmProviderCheck.withDefaults().verify(environment)` in `postProcessBeanFactory`); `@AutoConfiguration LlmProviderCheckAutoConfiguration` with `@Bean static LlmProviderCheckPostProcessor llmProviderCheckPostProcessor()`. Register both resource files. If a Boot 4 import moved, fix it by compiling, don't change the design.

- [ ] **Step 8:** `rtk ./mvnw -q -pl llm-provider-check verify` → PASS; `rtk ./mvnw -q verify` → whole build still PASS.

- [ ] **Step 9: Commit** `feat: add llm-provider-check module with fail-fast provider validation`.

---

### Task 2: rag-search-mcp — provider switch and per-provider table

**Files:**
- Modify: `rag-search-mcp/pom.xml` (add `spring-ai-starter-model-openai` and `com.springai.jdd:llm-provider-check:${project.version}`)
- Modify: `rag-search-mcp/src/main/resources/application.yaml`
- Modify: `rag-search-mcp/src/main/java/com/springai/jdd/rag/WikiIngestor.java` (constructor + `hashBytes`)
- Modify: `rag-search-mcp/src/test/java/com/springai/jdd/rag/WikiIngestorTest.java:72`
- Create: `rag-search-mcp/src/test/java/com/springai/jdd/rag/ProviderConfigTest.java`

**Interfaces:**
- Consumes: Task 1 module (auto-configured; no code reference).
- Produces: YAML key `llm.embedding-model` (resolved embedding model name for the active provider) and `llm.embedding-models.openai|ollama`; Task 3 mirrors the same keys. `WikiIngestor(VectorStore, ResourcePatternResolver, ChunkIndex, @Value("${llm.embedding-model}") String embeddingModel)`.

**YAML (rag-search-mcp):**
```yaml
llm:
  embedding-models:
    openai: ${OPENAI_EMBEDDING_MODEL:text-embedding-3-small}
    ollama: ${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}
  embedding-model: ${llm.embedding-models.${llm.provider:openai}}
spring.ai:
  model: { chat: none, embedding: "${llm.provider:openai}", image: none, audio.speech: none, audio.transcription: none, moderation: none }
  openai: { api-key: "${OPENAI_API_KEY:}", base-url: "${OPENAI_BASE_URL:https://api.openai.com/v1}", embedding.options: { model: "${llm.embedding-models.openai}", dimensions: 768 } }
  ollama.embedding.model: ${llm.embedding-models.ollama}     # base-url / init block unchanged
  vectorstore.pgvector.table-name: wiki_${llm.provider:openai}
```
(Write it in the file's existing block style, not flow style.)

- [ ] **Step 1: Write failing tests.**
  - `ProviderConfigTest`: helper loads `application.yaml` via `new YamlPropertySourceLoader().load("app", new ClassPathResource("application.yaml"))` into a `StandardEnvironment` plus a `MapPropertySource` of overrides, then `env.resolveRequiredPlaceholders(...)`/`getProperty`. Tests:
    - `shouldDefaultToOpenAi` — no overrides → `spring.ai.model.embedding`=`openai`, `table-name`=`wiki_openai`, `llm.embedding-model`=`text-embedding-3-small`, `spring.ai.openai.embedding.options.dimensions`=`768`, `spring.ai.model.image`=`none`.
    - `shouldSwitchToOllama` — `llm.provider=ollama` → `wiki_ollama`, `nomic-embed-text`, embedding `ollama`.
  - `WikiIngestorTest`: `shouldReembedWhenEmbeddingModelChanges` — ingest with model `a` then with model `b` against the same mocks; capture `vectorStore.add` documents; ids differ.
- [ ] **Step 2:** `rtk ./mvnw -q -pl rag-search-mcp -am test` → fails.
- [ ] **Step 3: Implement** YAML above; add `embeddingModel` to `WikiIngestor` and `digest.update(embeddingModel.getBytes(UTF_8))` after `CHUNKING_VERSION` in `hashBytes`; update the class comment and `WikiIngestorTest:72` to pass `"test-model"`.
- [ ] **Step 4:** `rtk ./mvnw -q -pl rag-search-mcp -am test` → PASS.
- [ ] **Step 5: Manual fail-fast check:** `env -u OPENAI_API_KEY ./mvnw -q -pl rag-search-mcp spring-boot:run` → output shows `APPLICATION FAILED TO START` with the key-missing Description/Action and **no** stack trace above it. Then `LLM_PROVIDER=ollama` with Ollama stopped → Ollama copy.
- [ ] **Step 6: Commit** `feat(rag-search): select embedding provider via LLM_PROVIDER, per-provider wiki table`.

---

### Task 3: chat-history-mcp — provider switch, per-provider table and threshold, model-tagged entries

**Files:**
- Modify: `chat-history-mcp/pom.xml` (same two dependencies as Task 2)
- Modify: `chat-history-mcp/src/main/resources/application.yaml`
- Modify: `chat-history-mcp/src/main/java/com/springai/jdd/chathistory/ChatHistoryTools.java`
- Modify: `chat-history-mcp/src/test/java/com/springai/jdd/chathistory/ChatHistoryToolsTest.java:30`
- Create: `chat-history-mcp/src/test/java/com/springai/jdd/chathistory/ProviderConfigTest.java`

**Interfaces:**
- Consumes: Task 2's YAML key names (`llm.embedding-models.*`, `llm.embedding-model`), duplicated here.
- Produces: `ChatHistoryTools(VectorStore, ObjectMapper, @Value("${chat-history.similarity-threshold}") double similarityThreshold, @Value("${llm.embedding-model}") String embeddingModel)`; metadata key `embedding_model`.

**YAML:** same `llm.*` and `spring.ai.*` blocks as Task 2, with `table-name: chat_history_${llm.provider:openai}`, plus:
```yaml
chat-history:
  similarity-thresholds:
    openai: 0.4
    ollama: 0.5
  similarity-threshold: ${CHAT_HISTORY_SIMILARITY_THRESHOLD:${chat-history.similarity-thresholds.${llm.provider:openai}}}
```

- [ ] **Step 1: Write failing tests.**
  - `ProviderConfigTest` (same helper as Task 2): `shouldDefaultToOpenAi` (`chat_history_openai`, threshold `0.4`), `shouldSwitchToOllama` (`chat_history_ollama`, `0.5`), `shouldLetEnvOverrideThreshold` (`CHAT_HISTORY_SIMILARITY_THRESHOLD=0.66` → `0.66`).
  - `ChatHistoryToolsTest`: `shouldTagRecordedEntryWithEmbeddingModel` — capture `add` → metadata `embedding_model` = `"test-model"`; `shouldOnlySearchEntriesFromCurrentEmbeddingModel` — capture `SearchRequest` → `getFilterExpression()` equals `new FilterExpressionBuilder().eq("embedding_model", "test-model").build()`.
- [ ] **Step 2:** `rtk ./mvnw -q -pl chat-history-mcp -am test` → fails.
- [ ] **Step 3: Implement** YAML; in `ChatHistoryTools` add the constructor param (drop the `:0.5` inline default — YAML now owns it), put `embedding_model` into the recorded metadata, and add `.filterExpression(...)` to the search request. Update `ChatHistoryToolsTest:30`.
- [ ] **Step 4:** `rtk ./mvnw -q -pl chat-history-mcp -am test` → PASS.
- [ ] **Step 5: Commit** `feat(chat-history): select embedding provider via LLM_PROVIDER, model-tagged cache entries`.

---

### Task 4: assistant-api — OpenAI default, gpt-4o-mini, per-provider hit threshold, pricing

**Files:**
- Modify: `assistant-api/pom.xml` (add `llm-provider-check`)
- Modify: `assistant-api/src/main/resources/application.yml:15-39,66-88`
- Modify: `assistant-api/src/test/java/com/springai/jdd/assistant/WiringTest.java`
- Create: `assistant-api/src/test/java/com/springai/jdd/assistant/OllamaWiringTest.java`

**Interfaces:** Consumes Task 1 (`llm.verify-connectivity`). `CacheProperties.hitThreshold()` is unchanged in Java; its value now comes from YAML.

**YAML changes:**
- `spring.ai.model`: `chat: ${llm.provider:openai}`, `embedding: none`, plus `image/audio.speech/audio.transcription/moderation: none`. Update the comment above it.
- `spring.ai.openai.api-key: ${OPENAI_API_KEY:}`; `chat.model: ${OPENAI_MODEL:gpt-4o-mini}`.
- `assistant.cache.hit-threshold: ${ASSISTANT_CACHE_HIT_THRESHOLD:${assistant.cache-hit-thresholds.${llm.provider:openai}}}` with sibling top-level block `assistant.cache-hit-thresholds: { openai: 0.8 (# guess — tune on real data), ollama: 0.9 }` — must not sit under `assistant.cache` (record binding).
- Pricing: add `"[gpt-4o-mini-2024-07-18]"` 0.15 / 0.60; re-price `"[qwen2.5:7b-instruct]"` to 0.15 / 0.60; comment says "priced at gpt-4o-mini rates". Keep the `gpt-4.1-mini-2025-04-14` entry.

- [ ] **Step 1: Write failing tests.**
  - `WiringTest`: replace `NO_MODEL_PULL` with `"llm.verify-connectivity=false"` and `"spring.ai.openai.api-key=sk-test-not-real"`. Add `shouldDefaultToOpenAiCacheThreshold` (`CacheProperties.hitThreshold()` == `0.8`) and `shouldPriceGpt4oMini` (`priceOf` usage with model `gpt-4o-mini-2024-07-18`, 1000 prompt / 75 completion → `0.000195`, compare with `isEqualByComparingTo`). Existing qwen pricing test stays.
  - `OllamaWiringTest`: `@SpringBootTest(webEnvironment = NONE)` with `llm.provider=ollama`, `llm.verify-connectivity=false`, `spring.ai.ollama.init.pull-model-strategy=never`; `shouldBuildChatClientOnOllama` (ChatClient not null, `ChatModel` bean is an `OllamaChatModel`) and `shouldUseOllamaCacheThreshold` (== `0.9`).
- [ ] **Step 2:** `rtk ./mvnw -q -pl assistant-api -am test` → new tests fail.
- [ ] **Step 3: Implement** the YAML changes and pom dependency.
- [ ] **Step 4:** `rtk ./mvnw -q -pl assistant-api -am test` → all assistant-api tests PASS (incl. `TokenPricingTest`, which has its own map).
- [ ] **Step 5: Commit** `feat(assistant-api): OpenAI gpt-4o-mini by default, per-provider cache threshold`.

---

### Task 5: Docker Compose, reset script, end-to-end runs

**Files:**
- Modify: `docker-compose.yml:10-14,60,75,105-117`
- Modify: `scripts/reset-demo.sh`

**Compose:** replace `x-ollama` with an `x-llm` anchor merged into `rag-search-mcp`, `chat-history-mcp`, `assistant-api`:
```yaml
x-llm: &llm-connection
  LLM_PROVIDER: ${LLM_PROVIDER:-openai}
  OPENAI_API_KEY: ${OPENAI_API_KEY:-}
  OPENAI_BASE_URL: ${OPENAI_BASE_URL:-https://api.openai.com/v1}
  OPENAI_EMBEDDING_MODEL: ${OPENAI_EMBEDDING_MODEL:-text-embedding-3-small}
  OLLAMA_BASE_URL: ${OLLAMA_BASE_URL:-http://host.docker.internal:11434}
  OLLAMA_EMBEDDING_MODEL: ${OLLAMA_EMBEDDING_MODEL:-nomic-embed-text}
```
Keep the existing host-vs-container Ollama comment on it. In `assistant-api`: `OPENAI_MODEL: ${OPENAI_MODEL:-gpt-4o-mini}`, drop the now-duplicate `LLM_PROVIDER`/`OPENAI_API_KEY` lines, and change `ASSISTANT_CACHE_HIT_THRESHOLD` to pass-through-only (`ASSISTANT_CACHE_HIT_THRESHOLD:` with no value). Same pass-through for `CHAT_HISTORY_SIMILARITY_THRESHOLD:` on chat-history-mcp. Keep `restart: unless-stopped`.

**reset-demo.sh:** replace `TRUNCATE chat_history;` with a `DO $$ ... $$` block that truncates each of `chat_history`, `chat_history_openai`, `chat_history_ollama` only `IF to_regclass('<name>') IS NOT NULL`. Leave the wiki-tables comment as is.

- [ ] **Step 1: Compose config check (Review Focus 4):** `env -u ASSISTANT_CACHE_HIT_THRESHOLD -u CHAT_HISTORY_SIMILARITY_THRESHOLD docker compose config | grep -E "THRESHOLD|LLM_PROVIDER"` → shows `LLM_PROVIDER: openai` for the three AI services and **no** threshold variables.
- [ ] **Step 2:** `docker compose build rag-search-mcp chat-history-mcp assistant-api conference-info-mcp web-search-mcp` → all succeed.
- [ ] **Step 3:** With `OPENAI_API_KEY` unset: `docker compose up -d postgres rag-search-mcp && docker compose logs rag-search-mcp | grep -A6 "APPLICATION FAILED TO START"` → key-missing copy.
- [ ] **Step 4:** With a real key (ask the user to provide it in their shell; never write it to a file): `docker compose up -d --build`, then `curl -s localhost:8090/actuator/health` is `UP`, `docker compose exec -T postgres psql -U jdd -d jdd -c "select count(*) from wiki_openai"` > 0, and one `POST /query` answers. Then run `./scripts/reset-demo.sh` → succeeds.
- [ ] **Step 5: Ollama run:** with host Ollama running, `LLM_PROVIDER=ollama docker compose up -d rag-search-mcp chat-history-mcp assistant-api` → all healthy, `select count(*) from wiki_ollama` > 0, one `POST /query` answers. Switch back with `docker compose up -d rag-search-mcp chat-history-mcp assistant-api` (no `LLM_PROVIDER`) → healthy again, `wiki_openai` untouched.
- [ ] **Step 6: Commit** `build: pass LLM provider settings to all AI services, reset per-provider cache tables`.

---

### Task 6: Docs and dashboard text

**Files:**
- Modify: `.env.example` — OpenAI section first (`LLM_PROVIDER=openai` default, `OPENAI_API_KEY`, `OPENAI_MODEL=gpt-4o-mini`, `OPENAI_EMBEDDING_MODEL`, `OPENAI_BASE_URL` with the note: *an OpenAI-compatible API works only if it also serves `text-embedding-3-small` with the `dimensions` option*), then "Local Ollama: `LLM_PROVIDER=ollama`" with `OLLAMA_*`; replace `EMBEDDING_MODEL`; add `LLM_VERIFY_CONNECTIVITY`, `CHAT_HISTORY_SIMILARITY_THRESHOLD`; cache-threshold comment says the default depends on provider.
- Modify: `README.md:6,10,40-43,60-80` — OpenAI is the default (prerequisite: `OPENAI_API_KEY`); Ollama prerequisites move under "Local Ollama (`LLM_PROVIDER=ollama`)"; the "Hosted LLM" section becomes "Switching provider" (all AI services restart together; per-provider tables); mention the fail-fast message.
- Modify: `CLAUDE.md` — "LLM provider" bullet: one `LLM_PROVIDER` (default `openai`) selects chat + embeddings in all three AI modules; `llm-provider-check` validates at startup; per-provider tables `wiki_<provider>`, `chat_history_<provider>`; module table gains `llm-provider-check`; table names updated in the module table.
- Modify: `docs/demo-script.md` — delete the "Swap the model" beat at line 168; reword line 113 to "Swapping the LLM is one property: `LLM_PROVIDER` switches chat and embeddings together"; line 128 → `gpt-4o-mini`; any `ollama list` pre-flight check becomes "`OPENAI_API_KEY` exported" (keep Ollama checks only under a clearly marked local-rehearsal note).
- Modify: `observability/grafana/dashboards/jdd-assistant.json:349` → `gpt-4o-mini rates`.
- Check: `rtk grep -n -i "ollama" docs/speaker-notes.md docs/index.html` — change only sentences stating Ollama is the default/only LLM; if `speaker-notes.md` changes, run `python3 notes_tool.py apply index.html speaker-notes.md` from `docs/`.

- [ ] **Step 1:** Make the edits above.
- [ ] **Step 2:** `rtk grep -rn "EMBEDDING_MODEL\b\|gpt-4.1-mini\|LLM_PROVIDER:ollama\|LLM_PROVIDER:-ollama" --exclude-dir=node_modules --exclude-dir=target .` → only the intentional `gpt-4.1-mini-2025-04-14` pricing entry remains.
- [ ] **Step 3:** `rtk ./mvnw -q verify` → PASS (final full build).
- [ ] **Step 4: Commit** `docs: OpenAI by default, Ollama for local runs; drop the swap-the-model beat`.
