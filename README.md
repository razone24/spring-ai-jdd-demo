# spring-ai-jdd-demo

Live demo for **"The Future of Software Architecture in the Age of AI"**, JDD 2026, Kraków.

The project is a conference assistant built on Spring AI 2. It is an agent with a production-style harness, running
on **OpenAI (gpt-4o-mini)** by default or a **local LLM (Ollama)** with one switch. It reaches its tools through
**four MCP servers** and is observable end to end in **Grafana**.

```
 Web UI ──► nginx ──► assistant-api ──── LLM (OpenAI · gpt-4o-mini  |  Ollama · qwen2.5:7b-instruct)
 (React)             (Spring AI agent)
                       │  harness: semantic cache · memory · grounding guard · round budget ·
                       │           refusal tool · rate limit · audit · metrics
                       │
                       ├─ MCP ─► conference-info-mcp   overview + filterable agenda
                       ├─ MCP ─► rag-search-mcp        wiki RAG ─┐
                       ├─ MCP ─► chat-history-mcp      semantic ─┼─► Postgres + pgvector
                       └─ MCP ─► web-search-mcp        Tavily    │   (memory, audit, vectors)
                                                                 │
                           Prometheus ◄── /actuator/prometheus   └──► Grafana
```

## Modules

| Module | Port | What it is |
| --- | --- | --- |
| `assistant-api` | 8090 | Spring AI agent: MCP client, harness, `POST /query` |
| `llm-provider-check` | – | Shared jar: stops startup with a clear message when the selected LLM provider isn't usable |
| `conference-info-mcp` | 8080 | MCP tools `getConferenceOverview`, `getConferenceSchedule(day, room, track, keyword)` |
| `rag-search-mcp` | 8081 | MCP tool `searchKnowledgeBase`: hybrid retrieval (pgvector + Postgres full-text, rank fusion) over `src/main/resources/wiki/*.md`, embedded at startup one chunk per section |
| `chat-history-mcp` | 8082 | MCP tools `searchChatHistory`, `recordChatHistory` (semantic cache) |
| `web-search-mcp` | 8083 | MCP tool `searchWeb` (Tavily). Degrades gracefully without a key |
| `webapp` | 5173 | React console: conversations, agent trace (model vs harness), tokens, cost, cache hits |
| `observability/` | 9090 / 3000 | Prometheus + Grafana, with a provisioned *JDD Assistant — agent activity* dashboard |

All MCP servers use **stateless Streamable HTTP** (`/mcp`), the current MCP transport.

## Prerequisites

- Java 25, Docker (with Compose)
- An OpenAI API key, exported as `OPENAI_API_KEY` (or put in `.env`)
- Optional: `cp .env.example .env` and set `TAVILY_API_KEY` for web search

For local runs without OpenAI, see [Local Ollama](#local-ollama).

## Run

```bash
docker compose up -d --build
```

| URL | |
| --- | --- |
| http://localhost:5173 | Assistant UI |
| http://localhost:3000 | Grafana (anonymous viewer, admin/admin) |
| http://localhost:9090 | Prometheus |

Before presenting, run `./scripts/warm-up.sh`. Between rehearsals, run `./scripts/reset-demo.sh`.
The run sheet is in [docs/demo-script.md](docs/demo-script.md).

### Choosing the LLM provider

One setting, `LLM_PROVIDER`, picks the provider for chat (`assistant-api`) and embeddings (`rag-search-mcp`,
`chat-history-mcp`) together:

| `LLM_PROVIDER` | Chat | Embeddings (768 dims) | Vector tables |
| --- | --- | --- | --- |
| `openai` (default) | `gpt-4o-mini` | `text-embedding-3-small` | `wiki_openai`, `chat_history_openai` |
| `ollama` | `qwen2.5:7b-instruct` | `nomic-embed-text` | `wiki_ollama`, `chat_history_ollama` |

Each provider has its own tables, so switching never mixes vectors from different models; the wiki is embedded on
the first start with a provider and the semantic cache starts empty. Switch all three AI services together.

If the selected provider isn't usable (no or rejected `OPENAI_API_KEY`, OpenAI or Ollama unreachable), each AI service
stops at startup with a short `APPLICATION FAILED TO START` report saying what to set. A rate limit or an OpenAI outage
(HTTP 429/5xx) only logs a warning. `LLM_VERIFY_CONNECTIVITY=false` skips the network check (the key must still be set).

### Local Ollama

Run [Ollama](https://ollama.com) on the host (Metal GPU on a Mac), pull the models, and select it:

```bash
ollama pull qwen2.5:7b-instruct
ollama pull nomic-embed-text
LLM_PROVIDER=ollama docker compose up -d --build
```

**Fully containerised Ollama.** This runs CPU-only on macOS, so it is noticeably slower:

```bash
LLM_PROVIDER=ollama OLLAMA_BASE_URL=http://ollama:11434 docker compose --profile ollama up -d --build
```

The Spring AI Ollama starter pulls any missing models on first start.

## Develop

```bash
./mvnw verify                                   # build + unit tests, all modules
./mvnw install -DskipTests                      # once, so single-module runs find the shared llm-provider-check
docker compose up -d postgres                   # local runs need Postgres (and OPENAI_API_KEY, or LLM_PROVIDER=ollama)
./mvnw spring-boot:run -pl rag-search-mcp       # any module
SPRING_PROFILES_ACTIVE=persistence ./mvnw spring-boot:run -pl assistant-api
cd webapp && npm ci && npm run dev              # UI on :5173, proxies /api to :8090
```

Without the `persistence` profile, `assistant-api` runs with in-memory chat memory and no audit tables.

## What the harness does (and where)

| Concern | Where |
| --- | --- |
| The four steps of one answer: cache → model + MCP tools → cache write-back → audit | `agent/OrchestratorAgent` |
| Agent = model + harness: system prompt, memory → grounding guard → bounded tool loop | `agent/AgentConfiguration` |
| Per-question state the advisors share (tools, trail, rounds, tokens, guards) | `agent/AgentTurn` |
| Semantic cache (opening questions only; 24 h max age; never refusals, empty, web or failed-tool answers) | `harness/SemanticCache` |
| Bounded tool loop + token tally | `harness/BoundedToolLoopAdvisor` |
| Grounding guard: an answer with no tool call is sent back once | `harness/GroundingAdvisor` |
| Refusal as a typed tool (`returnDirect`) + text fallback | `harness/RefusalTool`, `RefusalText` |
| MCP tool discovery per server, tracing, fail-fast connections, rejoin after a failure | `mcp/McpToolset`, `mcp/TracingToolCallback`, `mcp/McpClientConfiguration` |
| Audit (log, Micrometer, Postgres) that never fails the request | `audit/*` |
| Request id, rate limit per client, uniform error contract | `api/*` |

The agenda and practical details are **demo data**. JDD's official 2026 schedule was not yet published when this
repo was built.
