# spring-ai-jdd-demo

Live demo for **"The Future of Software Architecture in the Age of AI"**, JDD 2026, Kraków.

The project is a conference assistant built on Spring AI 2. It is an agent with a production-style harness, running
on a **local LLM (Ollama)**. It reaches its tools through **four MCP servers** and is observable end to end in
**Grafana**.

```
 Web UI ──► nginx ──► assistant-api ──── LLM (Ollama · qwen2.5:7b-instruct)
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
| `conference-info-mcp` | 8080 | MCP tools `getConferenceOverview`, `getConferenceSchedule(day, room, track, keyword)` |
| `rag-search-mcp` | 8081 | MCP tool `searchKnowledgeBase`: hybrid retrieval (pgvector + Postgres full-text, rank fusion) over `src/main/resources/wiki/*.md`, embedded at startup one chunk per section |
| `chat-history-mcp` | 8082 | MCP tools `searchChatHistory`, `recordChatHistory` (semantic cache) |
| `web-search-mcp` | 8083 | MCP tool `searchWeb` (Tavily). Degrades gracefully without a key |
| `webapp` | 5173 | React console: conversations, agent trace (model vs harness), tokens, cost, cache hits |
| `observability/` | 9090 / 3000 | Prometheus + Grafana, with a provisioned *JDD Assistant — agent activity* dashboard |

All MCP servers use **stateless Streamable HTTP** (`/mcp`), the current MCP transport.

## Prerequisites

- Java 25, Docker (with Compose)
- [Ollama](https://ollama.com) running on the host, with the models pulled:
  ```bash
  ollama pull qwen2.5:7b-instruct
  ollama pull nomic-embed-text
  ```
- Optional: `cp .env.example .env` and set `TAVILY_API_KEY` for web search

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

**Fully containerised LLM.** This runs CPU-only on macOS, so it is noticeably slower:

```bash
OLLAMA_BASE_URL=http://ollama:11434 docker compose --profile ollama up -d --build
```

The Spring AI Ollama starter pulls any missing models on first start.

**Hosted LLM instead.** This is a configuration change only:

```bash
LLM_PROVIDER=openai OPENAI_API_KEY=sk-... docker compose up -d assistant-api
```

## Develop

```bash
./mvnw verify                                   # build + unit tests, all modules
docker compose up -d postgres                   # local runs need Postgres (and host Ollama)
./mvnw spring-boot:run -pl rag-search-mcp       # any module
SPRING_PROFILES_ACTIVE=persistence ./mvnw spring-boot:run -pl assistant-api
cd webapp && npm ci && npm run dev              # UI on :5173, proxies /api to :8090
```

Without the `persistence` profile, `assistant-api` runs with in-memory chat memory and no audit tables.

## What the harness does (and where)

| Concern | Where |
| --- | --- |
| Semantic cache before and after the LLM (opening questions only; never refusals, web or failed-tool answers) | `agent/cache/SemanticCache`, `agent/QueryOrchestrator` |
| Bounded tool loop + token tally | `agent/chat/loop/RoundBoundedToolAdvisor`, `RoundBudget`, `TokenLedger` |
| Grounding guard: an answer with no tool call is sent back once | `agent/chat/loop/GroundingAdvisor` |
| Refusal as a typed tool (`returnDirect`) + text fallback | `agent/refusal/*` |
| Conversation memory (JDBC, windowed) | `agent/memory/MemoryConfiguration` |
| MCP tool discovery, per-server tracing, re-discovery after a failure | `agent/tool/McpToolset`, `TracingToolCallback` |
| Audit (log, Micrometer, Postgres) that never fails the request | `audit/*` |
| Request id, rate limit per client, uniform error contract | `api/*` |

The agenda and practical details are **demo data**. JDD's official 2026 schedule was not yet published when this
repo was built.
