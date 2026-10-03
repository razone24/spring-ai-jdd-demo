# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Live demo for the JDD 2026 (Kraków) talk "The Future of Software Architecture in the Age of AI". It is a Maven
multi-module monorepo: a Spring AI agent (`assistant-api`), four MCP servers, a React UI (`webapp/`) and an
observability stack (`observability/`). Stack: Java 25, Spring Boot 4.1, Spring AI 2.0, Ollama, Postgres + pgvector.

| Module | Port | Role |
| --- | --- | --- |
| `assistant-api` | 8090 | Agent + harness. MCP **client** of the four servers; `POST /query` |
| `conference-info-mcp` | 8080 | `getConferenceOverview`, `getConferenceSchedule(day, room, track, keyword)` (static data in `Agenda.java`) |
| `rag-search-mcp` | 8081 | `searchKnowledgeBase` over `src/main/resources/wiki/*.md` (pgvector table `wiki`; hybrid vector + full-text, `HybridRanking`) |
| `chat-history-mcp` | 8082 | `searchChatHistory`, `recordChatHistory` (pgvector table `chat_history`) |
| `web-search-mcp` | 8083 | `searchWeb` (Tavily; returns a "not configured" result without `TAVILY_API_KEY`) |
| `webapp` | 5173 | React 19 + Vite; nginx proxies `/api/*` to `assistant-api:8090` |

## Commands

```bash
./mvnw verify                                  # all modules, unit tests
./mvnw test -pl assistant-api                  # one module
docker compose up -d --build                   # full stack (host Ollama on :11434)
docker compose up -d --build assistant-api     # rebuild one service
./scripts/reset-demo.sh                        # clear cache, chat memory, audit
cd webapp && npm run build && npm run lint     # UI checks
```

## Architecture notes

- **MCP transport.** Every server uses `spring.ai.mcp.server.protocol: STATELESS` (Streamable HTTP on `/mcp`).
  Tools are registered the same way everywhere: a `@Configuration` exposes `List<ToolCallback>` built with
  `ToolCallbacks.from(toolsBean)`.
- **Agent tool wiring.** The MCP client's auto tool-callback provider is off
  (`spring.ai.mcp.client.toolcallback.enabled: false`). `McpToolset` lists tools per `McpSyncClient`, tags each
  with its server name, wraps it in `TracingToolCallback`, and re-discovers tools after a failure. Clients
  initialise lazily (`initialized: false`), so the agent starts even if a server is down.
- **Packages mirror the talk.** `agent` (the flow), `harness` (guardrails around the model), `mcp` (tools + trace),
  plus `api` and `audit` plumbing.
- **Harness vs model.** `OrchestratorAgent.answer()` is four steps: semantic-cache lookup, the model with its MCP
  tools, cache write-back, then pricing and audit. Only step 2 is a model decision. The cache steps call the
  `HARNESS_ONLY` tools through `McpToolset.callAsHarness`. `AgentTurns.start()` gives the model every other MCP
  tool plus the local `refuse` tool.
- **Advisor chain.** `AgentConfiguration` builds the `ChatClient` with `MessageChatMemoryAdvisor` →
  `GroundingAdvisor` → `BoundedToolLoopAdvisor` (the tool loop). All per-question state lives in one `AgentTurn`,
  passed as the `AgentTurn.KEY` advisor param and read with `AgentTurn.of(context)`.
- **Failure behaviour.** MCP connections fail fast (2 s connect timeout, `McpClientConfiguration`). A failed tool
  reaches the model as "unavailable, do not guess", and answers built from a failed tool are never cached. The
  semantic cache also ignores entries older than `assistant.cache.max-age` (24 h). nginx re-resolves
  `assistant-api` through Docker DNS, so recreating that container doesn't break the UI.
- **LLM provider.** `spring.ai.model.chat` selects it (`ollama` by default, `openai` for any OpenAI-compatible
  API). Both starters are on the classpath. Embeddings in the MCP servers use the Ollama starter
  (`nomic-embed-text`, 768 dimensions).
- **Persistence.** One Postgres (`pgvector/pgvector:pg17`). Flyway in `assistant-api` owns `spring_ai_chat_memory`,
  `query_audit` and `tool_call_audit` (`baseline-on-migrate`, because the vector tables share the schema).
  Extensions are created in `observability/postgres/init/01-init.sql`.
- **Wiki ingest.** Chunk ids are `sha256(CHUNKING_VERSION + file bytes)[0:16]-<section>`. Unchanged files are
  skipped, and chunks of changed or deleted files are pruned. Bump `CHUNKING_VERSION` when the chunking changes.
- **Metrics.** Custom meters are `assistant.*` (`audit/metrics/Metrics.java`). Spring AI's own observations
  (`gen_ai_client_*`, `spring_ai_tool_*`, `db_vector_*`) are exported too. The Grafana dashboard is generated JSON
  in `observability/grafana/dashboards/jdd-assistant.json`.

## Testing approach

Unit tests only (JUnit 5, Mockito, AssertJ, MockMvc); no Testcontainers. `OrchestratorAgentTest` drives a real
`ChatClient`, built by `AgentConfiguration`, with a mocked `ChatModel`, so advisors, memory and the tool loop run for
real.

## Conference presentation (`docs/`)

`docs/index.html` is a reveal.js deck (diagrams and photos in `docs/presentation/`). Speaker notes live in
`docs/speaker-notes.md` and are synced with `docs/notes_tool.py`:

```bash
# from docs/
python3 notes_tool.py extract index.html -o speaker-notes.md
python3 notes_tool.py apply index.html speaker-notes.md
```

Edit notes in `speaker-notes.md` and run `apply`; don't hand-edit the `<aside class="notes">` blocks.
The live-demo run sheet is `docs/demo-script.md`.

# RTK (Rust Token Killer) - Token-Optimized Commands

## Golden Rule

**Always prefix commands with `rtk`**. If RTK has a dedicated filter, it uses it. If not, it passes through unchanged. This means RTK is always safe to use.

**Important**: Even in command chains with `&&`, use `rtk`:
```bash
# ❌ Wrong
git add . && git commit -m "msg" && git push

# ✅ Correct
rtk git add . && rtk git commit -m "msg" && rtk git push
```

## RTK Commands by Workflow

### Build & Compile (80-90% savings)
```bash
rtk cargo build         # Cargo build output
rtk cargo check         # Cargo check output
rtk cargo clippy        # Clippy warnings grouped by file (80%)
rtk tsc                 # TypeScript errors grouped by file/code (83%)
rtk lint                # ESLint/Biome violations grouped (84%)
rtk prettier --check    # Files needing format only (70%)
rtk next build          # Next.js build with route metrics (87%)
```

### Test (60-99% savings)
```bash
rtk cargo test          # Cargo test failures only (90%)
rtk go test             # Go test failures only (90%)
rtk jest                # Jest failures only (99.5%)
rtk vitest              # Vitest failures only (99.5%)
rtk playwright test     # Playwright failures only (94%)
rtk pytest              # Python test failures only (90%)
rtk rake test           # Ruby test failures only (90%)
rtk rspec               # RSpec test failures only (60%)
rtk test <cmd>          # Generic test wrapper - failures only
```

### Git (59-80% savings)
```bash
rtk git status          # Compact status
rtk git log             # Compact log (works with all git flags)
rtk git diff            # Compact diff (80%)
rtk git show            # Compact show (80%)
rtk git add             # Ultra-compact confirmations (59%)
rtk git commit          # Ultra-compact confirmations (59%)
rtk git push            # Ultra-compact confirmations
rtk git pull            # Ultra-compact confirmations
rtk git branch          # Compact branch list
rtk git fetch           # Compact fetch
rtk git stash           # Compact stash
rtk git worktree        # Compact worktree
```

Note: Git passthrough works for ALL subcommands, even those not explicitly listed.

### GitHub (26-87% savings)
```bash
rtk gh pr view <num>    # Compact PR view (87%)
rtk gh pr checks        # Compact PR checks (79%)
rtk gh run list         # Compact workflow runs (82%)
rtk gh issue list       # Compact issue list (80%)
rtk gh api              # Compact API responses (26%)
```

### JavaScript/TypeScript Tooling (70-90% savings)
```bash
rtk pnpm list           # Compact dependency tree (70%)
rtk pnpm outdated       # Compact outdated packages (80%)
rtk pnpm install        # Compact install output (90%)
rtk npm run <script>    # Compact npm script output
rtk npx <cmd>           # Compact npx command output
rtk prisma              # Prisma without ASCII art (88%)
```

### Files & Search (60-75% savings)
```bash
rtk ls <path>           # Tree format, compact (65%)
rtk read <file>         # Code reading with filtering (60%)
rtk grep <pattern>      # Search grouped by file (75%). Format flags (-c, -l, -L, -o, -Z) run raw.
rtk find <pattern>      # Find grouped by directory (70%)
```

### Analysis & Debug (70-90% savings)
```bash
rtk err <cmd>           # Filter errors only from any command
rtk log <file>          # Deduplicated logs with counts
rtk json <file>         # JSON structure without values
rtk deps                # Dependency overview
rtk env                 # Environment variables compact
rtk summary <cmd>       # Smart summary of command output
rtk diff                # Ultra-compact diffs
```

### Infrastructure (85% savings)
```bash
rtk docker ps           # Compact container list
rtk docker images       # Compact image list
rtk docker logs <c>     # Deduplicated logs
rtk kubectl get         # Compact resource list
rtk kubectl logs        # Deduplicated pod logs
```

### Network (65-70% savings)
```bash
rtk curl <url>          # Compact HTTP responses (70%)
rtk wget <url>          # Compact download output (65%)
```

### Meta Commands
```bash
rtk gain                # View token savings statistics
rtk gain --history      # View command history with savings
rtk discover            # Analyze Claude Code sessions for missed RTK usage
rtk proxy <cmd>         # Run command without filtering (for debugging)
rtk init                # Add RTK instructions to CLAUDE.md
rtk init --global       # Add RTK to ~/.claude/CLAUDE.md
```

## Token Savings Overview

| Category | Commands | Typical Savings |
|----------|----------|-----------------|
| Tests | vitest, playwright, cargo test | 90-99% |
| Build | next, tsc, lint, prettier | 70-87% |
| Git | status, log, diff, add, commit | 59-80% |
| GitHub | gh pr, gh run, gh issue | 26-87% |
| Package Managers | pnpm, npm, npx | 70-90% |
| Files | ls, read, grep, find | 60-75% |
| Infrastructure | docker, kubectl | 85% |
| Network | curl, wget | 65-70% |

Overall average: **60-90% token reduction** on common development operations.
<!-- /rtk-instructions -->
