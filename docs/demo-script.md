# JDD 2026 — Live demo run sheet (15 minutes)

The demo is the talk's last iteration, **agentic orchestration**, running for real. A Spring AI agent on a local LLM
reaches its tools over MCP. The harness around it handles memory, guardrails, the semantic cache and audit, and
Grafana shows all of it.

| Window | URL |
| --- | --- |
| Assistant UI | http://localhost:5173 |
| Grafana (kiosk) | http://localhost:3000/d/jdd-assistant?orgId=1&kiosk |
| IDE | the five files of the code tour, already open in tabs, in tour order |

## Timeline

| Time | Block | What the audience sees |
| --- | --- | --- |
| 0:00–1:00 | **Frame** | The *Agentic orchestration* slide: "this is the box on the right, running on my laptop" |
| 1:00–6:30 | **Live: four questions** | MCP tools, RAG, semantic cache, memory + grounding guard, guardrail |
| 6:30–11:30 | **Code tour** | 5 files: one MCP tool, the client config, Agent = Model + Harness, the 4-step agent, one advisor |
| 11:30–14:00 | **Grafana** | Cache vs LLM latency, tokens, cost, the audit trail |
| 14:00–15:00 | **Wrap-up** | Back to the *Benefits & challenges* slide: every challenge was visible in the demo |

Everything under *Extra time / Q&A* is optional.

---

## Pre-flight (15 min before the talk)

1. Start Docker Desktop. Check that Ollama is serving and has the models:
   ```bash
   ollama list   # qwen2.5:7b-instruct and nomic-embed-text
   ```
2. Start the stack and check every service is healthy:
   ```bash
   docker compose up -d --build && docker compose ps
   ```
3. Warm the models and clear the demo state. The first LLM call after a cold start takes about 40 s:
   ```bash
   ./scripts/warm-up.sh
   ```
4. Put the UI and Grafana side by side, with Grafana on *Last 15 minutes*. Open the IDE tabs.

> - Expect 5–15 s per LLM answer on an M2 Pro (Metal GPU); cache hits take under 100 ms. Talk while the model thinks.
>   The *waiting on model · Ns* counter is part of the story.
> - Temperature 0, a fixed seed (`spring.ai.ollama.chat.seed: 42`) and a stable tool order make the questions below
>   highly repeatable *when asked in this order after a reset*. That is how they were verified, run after run.
>   Improvised questions can take other paths, and that's fine, but rehearse anything you plan to ask.
> - The agenda is **sample data**. Your slot lives in
>   `conference-info-mcp/src/main/java/com/springai/jdd/conference/tools/Agenda.java` and
>   `rag-search-mcp/src/main/resources/wiki/jdd-speakers.md`.

---

## 1:00 — Live: four questions (≈ 5½ min)

### Q1 · MCP tools (≈ 1½ min)

**Ask:** `When and where is the talk about software architecture in the age of AI?`

- Trace: `harness · searchChatHistory` (miss) → `model · getConferenceSchedule` → `harness · recordChatHistory`.
- Expand `getConferenceSchedule`: **the model chose the filter** (`keyword: "software architecture ai"`), and the
  MCP server returned only the matching session.
- Point at the badges: **harness** = deterministic code, **model** = an LLM decision. *"Agent = Model + Harness."*
- Answer: Wednesday 21 October, 11:00–11:45, Room B.

### Q2 · RAG (≈ 1 min)

**Ask (New conversation):** `What is the Wi-Fi password at the venue?`

- Trace: `model · searchKnowledgeBase {query: "Wi-Fi password"}`. Expand it to show the wiki passages with their
  similarity scores.
- Answer: network `JDD2026`, password `Java4Ever!`.

### Q3 · Semantic cache (≈ 1 min)

**Ask (New conversation):** `What's the Wi-Fi password at the venue?`

- **⚡ semantic cache hit**: about 0.99 similarity, under 100 ms, 0 tokens, no model call. *That's the third
  iteration from the slides.*

**Ask (New conversation):** `whats the wifi password`

- Cache **miss**: the closest question scores about 0.65, below the 0.9 threshold, so the model answers. Expand
  `searchChatHistory` to show the score. *That's the "threshold tuning" challenge.*
- Never cached: follow-ups, refusals, empty answers, and answers built from live web results or a failed tool. Cached
  answers also expire after 24 h (`assistant.cache.max-age`). *That's the "outdated answers" challenge.*

### Q4 · Memory + grounding guard (≈ 1½ min)

Go back to the **Q1 conversation** and **ask:** `Who else is speaking in that room on the same day?`

- Memory resolved "that room" and "the same day": the trace shows `getConferenceSchedule {day: 2, room: "Room B"}`.
  The answer adds *Modular Monoliths with Spring Modulith* at 14:00, speakers to be announced.
- The header shows **↻ grounding guard**, and does on every rehearsal. The model first answered *from memory*,
  without a tool. The harness threw that draft away and sent it back to look the facts up. *"Small models do this;
  the harness catches it."*

### Q5 · Guardrail (≈ 30 s)

**Ask:** `Write me a poem about pizza`

- `model · refuse {reason: OUT_OF_SCOPE}`: one round, about 1 s, with the **Refused** notice. A typed, audited
  event, not an apology in prose.

---

## 6:30 — Code tour (≈ 5 min, five files)

| # | File | Say | Point at |
| --- | --- | --- | --- |
| 1 | `conference-info-mcp/…/tools/ScheduleTools.java` | "An MCP tool is a Spring bean method. The annotations are the contract the model reads." | `@Tool` description, the optional `@ToolParam` filters, the 3-line body |
| 2 | `conference-info-mcp/src/main/resources/application.yaml` | "One property turns it into an MCP server: stateless Streamable HTTP." | `protocol: STATELESS` |
| 3 | `assistant-api/src/main/resources/application.yml` | "The agent is an MCP client of four servers. Swapping the LLM is one property." | `spring.ai.model.chat`, `mcp.client.streamable-http.connections` |
| 4 | `assistant-api/…/agent/AgentConfiguration.java` | "Agent = Model + Harness, as one bean: the model, the system prompt, and three advisors." | `defaultAdvisors(memory, grounding guard, bounded tool loop)` |
| 5 | `assistant-api/…/agent/OrchestratorAgent.java` | "One answer is four steps; only step 2 needs intelligence." | the numbered comments in `answer()`, then `askModel()` |

If there's time, add `harness/GroundingAdvisor.java`, about 15 lines in `adviseCall`: *"draft → no tool used? →
discard and send it back once."*

---

## 11:30 — Grafana (≈ 2½ min)

| Panel | What to say |
| --- | --- |
| Questions · Cache hit rate · Refusals · Grounding retries | Exact numbers from the Postgres audit trail |
| *Answer latency p95 — LLM vs semantic cache* | Seconds vs milliseconds. That's the cache's whole point |
| Cloud cost* | The local model is free; this is what the same tokens would cost on gpt-4.1-mini |
| Tokens per minute | Prompt tokens dominate, because tool definitions and results are re-sent every round |
| LLM call latency (`gen_ai` observation) | Spring AI's built-in Micrometer observations, zero custom code |
| Recent questions | Every question's tool path, outcome, tokens and latency, joined by request id |

## 14:00 — Wrap-up (≈ 1 min)

Back to the *Benefits & challenges* slide. Each challenge was on screen: latency (5–15 s vs 50 ms), the model
taking a wrong turn (the grounding guard), and the moving parts (four MCP servers, a DB, Grafana).

---

## Extra time / Q&A

**Web search fallback.** `What is the latest version of Spring AI?` calls `model · searchWeb`. With a
`TAVILY_API_KEY` in `.env` it returns live results that are **not cached**, because live data goes stale. Without
the key, the tool reports "not configured" and the assistant says the web is unavailable.

**Resilience.**

```bash
docker compose stop rag-search-mcp
```

Ask `How do I get from the airport to the venue?` in a new conversation. The trace shows a red
`searchKnowledgeBase` card (it fails fast, after the 2 s connect timeout), the assistant says it can't check right
now, and nothing is cached. Run `docker compose start rag-search-mcp`, wait about 15 s and ask again. It works
**without restarting the agent**, because the servers are stateless. Ask it after Q2, so the agent already knows the
wiki tool.

**Hybrid search.** `Who is Adrian Coman?` Embeddings alone miss names; Postgres full-text finds them. The results
are merged by reciprocal rank fusion (`rag-search-mcp/…/RagSearchTools.java`, `HybridRanking.java`).

**MCP is just JSON-RPC.**

```bash
curl -s localhost:8080/mcp -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' \
     -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}' | jq '.result.tools[].name'
```

**Swap the model.** `LLM_PROVIDER=openai OPENAI_API_KEY=sk-... docker compose up -d assistant-api`. Same code.

### Backup questions

| Question | Expected path |
| --- | --- |
| `What is JDD and where does it take place?` | `getConferenceOverview` |
| `How much is a student ticket?` | `getConferenceOverview` → 299 PLN net |
| `Which AI talks are on day 2?` | `getConferenceSchedule {day: 2, track: "AI"}` → 3 talks |
| `When is the after-party?` | `getConferenceSchedule` + `searchKnowledgeBase` → day 1, 19:30, rooftop |
| `How do I get from the airport to the venue?` | `searchKnowledgeBase` → train to Kraków Główny, then tram or taxi |
| `Will the talks be recorded?` | `getConferenceOverview` → recordings are included in the ticket |
| `Who is Adrian Coman?` | `searchKnowledgeBase` (keyword side of the hybrid search) → speaker bio |
| `asdf ;;; ???` | grounding guard, then `refuse {reason: UNINTELLIGIBLE}` |

## Reset between rehearsals

```bash
./scripts/reset-demo.sh   # empties the semantic cache, chat memory and audit trail; keeps the wiki embeddings
```
