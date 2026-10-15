# LLM provider selection and fail-fast startup

Date: 2026-10-12
Status: ready for review

## Why

Running `rag-search-mcp` without Ollama on `localhost:11434` dies with a ~100-line `UnsatisfiedDependencyException`
chain (`ragSearchTools` → `vectorStore` → `ollamaEmbeddingModel` → `Connection refused`). The cause is buried at the
bottom. Two things are wrong:

1. The demo is effectively Ollama-only. `assistant-api` can already switch its chat model, but the two embedding
   servers (`rag-search-mcp`, `chat-history-mcp`) only ship the Ollama starter.
2. Nothing checks the LLM setup up front, so a missing provider surfaces as an opaque bean-creation failure.

The owner develops locally against Ollama and presents on stage (JDD 2026) against OpenAI.

## Goals

- **OpenAI is the default provider** for every module that uses a model: `assistant-api`, `rag-search-mcp`,
  `chat-history-mcp`. Chat model `gpt-4o-mini`.
- **Ollama stays a first-class option**, chosen with one setting: `LLM_PROVIDER=ollama`. That one setting switches
  both chat and embeddings in every module.
- **Fail fast with a clear message.** If the chosen provider isn't usable, the app refuses to start and prints a short
  Spring Boot failure report (Description / Action) naming what's wrong and what to set — no stack-trace wall.
- Local Ollama data and on-stage OpenAI data never mix, and switching back and forth needs no manual DB work.

## Non-goals

- Other providers (Anthropic, Azure, Gemini…). The design should not make them hard, but they are not built now.
- Mixing providers (e.g. OpenAI chat + Ollama embeddings). One switch, one provider.
- Migrating existing vectors between providers. Each provider re-embeds its own data.
- Changes to `conference-info-mcp` and `web-search-mcp` (no models there).

## Anti-goals (would count as failure even if it "works")

- The app starts "successfully" with a placeholder key (today's `OPENAI_API_KEY:not-set`) and only breaks on the first
  question — on stage.
- Vectors from one embedding model are searched with queries from another (silent garbage results).
- The fail-fast message is still buried inside a long nested exception.

## Decisions made with the owner

- OpenAI default; Ollama kept as an option for local testing.
- `gpt-4o-mini` as the OpenAI chat model.
- Fail fast when the provider is not set/available, with a clear error.
- The semantic cache starting empty on stage (because OpenAI data is separate from local Ollama data) is fine.
- The live "Swap the model" demo beat is dropped; no chat-only override is needed.

## Design

### 1. One provider switch

`LLM_PROVIDER` (values `openai` | `ollama`, default `openai`) drives:

| Property | assistant-api | rag-search-mcp | chat-history-mcp |
|---|---|---|---|
| `spring.ai.model.chat` | `${LLM_PROVIDER:openai}` | `none` | `none` |
| `spring.ai.model.embedding` | `none` | `${LLM_PROVIDER:openai}` | `${LLM_PROVIDER:openai}` |

Spring AI 2.0.1 activates the matching auto-configuration from these properties, so both starters can sit on the
classpath. `rag-search-mcp` and `chat-history-mcp` gain `spring-ai-starter-model-openai`.

Any other value (including empty) is a startup failure (see §3).

### 2. Models and embedding dimensions

| | OpenAI | Ollama |
|---|---|---|
| Chat | `${OPENAI_MODEL:gpt-4o-mini}` | `${OLLAMA_MODEL:qwen2.5:7b-instruct}` (unchanged) |
| Embedding | `${OPENAI_EMBEDDING_MODEL:text-embedding-3-small}` | `${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}` |
| Dimensions | 768 (requested via `spring.ai.openai.embedding.options.dimensions`) | 768 (native) |

*Our call:* request 768-dim vectors from `text-embedding-3-small` so the pgvector column shape is the same for both
providers and `EMBEDDING_DIMENSIONS` needs no per-provider default. Quality loss at 768 is negligible for a 3-file
wiki. The existing `EMBEDDING_MODEL` env var is split into the two provider-specific ones above (keep `EMBEDDING_MODEL`
working as a fallback for the Ollama one only if trivial; otherwise drop it and update docs).

### 3. Fail-fast provider check (new shared module)

New Maven module `llm-provider-check` (plain jar, no Boot repackage), added to the parent `<modules>` and depended on
by `assistant-api`, `rag-search-mcp`, `chat-history-mcp`. Registered via
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` and/or `spring.factories` as needed.

It must run **before any Spring AI model bean is created** (the Ollama embedding model contacts the server in its
constructor). It checks, in order:

1. **Provider set and known.** `LLM_PROVIDER` resolves to `openai` or `ollama`.
2. **OpenAI:** `spring.ai.openai.api-key` is non-blank and not a placeholder (`not-set`, `sk-...`, `changeme`).
   Then one `GET {base-url}/models` with a short timeout (~5s): 401/403 → "key rejected"; connection failure/timeout →
   "OpenAI not reachable at {base-url}".
3. **Ollama:** one `GET {base-url}/api/tags` with a short timeout (~2s). Connection failure → "Ollama is not reachable
   at {base-url}". (Model pulling stays with Spring AI's `pull-model-strategy: when_missing`.)

*Our call on OpenAI-compatible gateways:* a `404` from `/models` counts as "cannot verify, continue" (logged at WARN),
so gateways without that endpoint still start. Only 401/403 and connection failures stop startup. Docs say
`OPENAI_BASE_URL` can point at an OpenAI-compatible API only if it also serves `text-embedding-3-small` with the
`dimensions` option; otherwise use real OpenAI.

The network checks can be switched off with `llm.provider.verify-connectivity=false` (for offline tests/CI). The
presence/format checks always run.

Failures throw a dedicated exception (e.g. `LlmProviderNotAvailableException`) handled by a Spring Boot
`FailureAnalyzer`, producing output like:

```
***************************
APPLICATION FAILED TO START
***************************

Description:

LLM provider 'openai' is selected but OPENAI_API_KEY is not set.

Action:

Set OPENAI_API_KEY, or run with LLM_PROVIDER=ollama to use a local Ollama at http://localhost:11434.
```

Each failure case gets its own Description/Action text. Message must never print the API key value.

Left to the builder: the exact hook (EnvironmentPostProcessor, BeanFactoryPostProcessor, or an early-initialised bean
that model beans depend on), as long as it runs before model bean creation and the FailureAnalyzer output is what the
user sees.

### 4. Separate vector tables per provider

- `rag-search-mcp`: `table-name: wiki_${LLM_PROVIDER:openai}` → `wiki_openai` / `wiki_ollama`.
- `chat-history-mcp`: `table-name: chat_history_${LLM_PROVIDER:openai}`.
- Existing `wiki` / `chat_history` tables are left in place (harmless); `scripts/reset-demo.sh` should drop the new
  tables (check what it currently drops and extend it).
- `JdbcChunkIndex` and `KeywordSearch` already read the table name from config — verify they still do after the change.
- `WikiIngestor`'s chunk-ID hash additionally includes the embedding model name, so changing the model within one
  provider also triggers re-embedding.
- *Our call:* `chat-history-mcp` stores the embedding model name in each entry's metadata (`embedding_model`) and
  filters `searchChatHistory` on the current model, so changing the model within one provider never compares vectors
  from different models. Old entries are simply never matched; `reset-demo.sh` clears them.

### 5. Per-provider similarity thresholds

OpenAI cosine scores are distributed differently from nomic's. Thresholds become per-provider, keeping the existing
env overrides on top:

- `chat-history.similarity-threshold` (today 0.5) — chat-history-mcp
- `assistant.cache.hit-threshold` (today 0.9) — assistant-api

Shape (builder's choice) e.g. `assistant.cache.hit-threshold.openai/ollama`, selected by `LLM_PROVIDER`. Ollama keeps
today's values. OpenAI starting values are a guess (suggest 0.4 floor / 0.8 hit for `text-embedding-3-small`) — mark
them with a comment saying they need tuning on real data.

### 6. assistant-api specifics

- `spring.ai.model.chat: ${LLM_PROVIDER:openai}`; OpenAI `api-key: ${OPENAI_API_KEY:}` (no `not-set` placeholder).
- Pricing: add the id OpenAI reports for `gpt-4o-mini` (currently `gpt-4o-mini-2024-07-18`; confirm from a real
  response's metadata) at $0.15 input / $0.60 output per million. Keep the `gpt-4.1-mini` entry.
- *Our call:* the local model's "cloud-equivalent" price follows the cloud model the demo actually uses: re-price
  `qwen2.5:7b-instruct` at `gpt-4o-mini` rates ($0.15 / $0.60) and update the comment, the Grafana panel text and
  `demo-script.md:128` ("what the same tokens would cost on gpt-4o-mini").
- `WiringTest`: must load the context without a network and without a real key — set
  `llm.provider.verify-connectivity=false` and a dummy key, and stop relying on the Ollama-only property where possible.
  Add a test per provider value if cheap.

### 7. Docker, docs, scripts

- `docker-compose.yml`:
  - `LLM_PROVIDER: ${LLM_PROVIDER:-openai}`, `OPENAI_API_KEY: ${OPENAI_API_KEY:-}`, `OPENAI_BASE_URL`, model vars and
    Ollama vars passed to all three AI services (replace the `x-ollama` anchor with an `x-llm` anchor).
  - Note: with `restart: unless-stopped`, a misconfigured service will restart-loop, printing the clear message each
    time. Acceptable; builder may switch these three to `restart: on-failure:3` if it's cleaner.
- All Dockerfiles copy every module's `pom.xml`, so each must also copy `llm-provider-check/pom.xml`, and the three AI
  modules' Dockerfiles must copy `llm-provider-check/src/`.
- `.env.example`, `README.md`, `CLAUDE.md` (inner), `docs/demo-script.md`: OpenAI is the default; local Ollama via
  `LLM_PROVIDER=ollama`; prerequisites split per provider. The demo-script "Swap the model" beat
  (`demo-script.md:168`) is **removed** (owner's decision): the stage demo runs on OpenAI throughout. The
  "swapping the LLM is one property" line at `demo-script.md:113` may stay as a spoken point, reworded to say one
  property (`LLM_PROVIDER`) switches chat and embeddings together.
- `docs/speaker-notes.md` / `docs/presentation/`: update only lines that state Ollama is the default.

## Tests / acceptance

1. `./mvnw verify` passes with no network and no `OPENAI_API_KEY`.
2. Unit tests for the check: unknown provider, blank key, placeholder key, OpenAI 401, OpenAI unreachable, Ollama
   unreachable → each yields the expected FailureAnalysis description/action (use a stub HTTP server or mock client).
3. Manual: `./mvnw spring-boot:run -pl rag-search-mcp` with nothing set → the APPLICATION FAILED TO START block naming
   `OPENAI_API_KEY`, no stack trace above it.
4. Manual: `LLM_PROVIDER=ollama` with Ollama stopped → clear "Ollama is not reachable" message.
5. Manual: `docker compose up` with a real key → all services healthy, `wiki_openai` populated, a query answered by
   `gpt-4o-mini`, cost shown on the Grafana dashboard.
6. Manual: `LLM_PROVIDER=ollama` with Ollama running → `wiki_ollama` populated, works as before.
