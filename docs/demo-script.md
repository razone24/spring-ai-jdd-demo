# JDD 2026 — Live demo run sheet

The demo follows the talk's last iteration, **agentic orchestration**. A Spring AI agent runs on a local LLM and
reaches its tools over MCP. The harness around it handles memory, guardrails, the semantic cache and audit, and
Grafana shows all of it. Each act maps to a slide.

| Window | URL |
| --- | --- |
| Assistant UI | http://localhost:5173 |
| Grafana (kiosk) | http://localhost:3000/d/jdd-assistant?orgId=1&kiosk |
| Slides | `docs/index.html` |

---

## Pre-flight (15 min before the talk)

1. Start Docker Desktop. Check that Ollama is serving and has the models:
   ```bash
   ollama list   # qwen2.5:7b-instruct and nomic-embed-text
   ```
2. Start the stack and wait until every service is healthy:
   ```bash
   docker compose up -d --build
   docker compose ps
   ```
3. Warm the models and clear the demo state. The first LLM call after a cold start takes about 40 s:
   ```bash
   ./scripts/warm-up.sh
   ```
4. Open the UI and Grafana side by side. Set the Grafana time range to *Last 15 minutes*.
5. Optional: in `.env`, set `ASSISTANT_TODAY=2026-10-21` (talk day) so questions like "today" or "after lunch" resolve
   correctly. Restart with `docker compose up -d assistant-api`.
6. Optional: set `TAVILY_API_KEY` in `.env` for Act 5. Without it, the act shows graceful degradation instead.

> Expect 5–15 s per LLM answer on an M2 Pro (Metal GPU). Cache hits come back in under 100 ms.
> Temperature 0 plus a fixed seed (`spring.ai.ollama.chat.seed: 42`) makes the rehearsed questions below take the same
> tool path every time. Reset the demo state before going on stage, because a cached answer short-circuits the path.
> Long, multi-part questions are where a 7B model slips, so keep questions short and single-intent.
> The demo agenda is **sample data**; the speaker slot lives in
> `conference-info-mcp/.../Agenda.java` and `rag-search-mcp/src/main/resources/wiki/jdd-speakers.md`.

---

## Act 1 — Tools over MCP (slide: *MCP — Model Context Protocol*)

**Ask:** `When and where is the talk about software architecture in the age of AI?`

- Expected trace: `harness · searchChatHistory` (cache miss), then `model · getConferenceSchedule`, then
  `harness · recordChatHistory`.
- Expand the `getConferenceSchedule` card. The **model chose the filters** (e.g. `keyword: "AI"`), and the MCP server
  returned only the matching sessions.
- Point at the badges: **harness** steps are deterministic code; **model** steps are LLM decisions.
- Answer: Wednesday 21 October, 11:00–11:45, Room B.

## Act 2 — RAG (slide: *Retrieval-Augmented Generation*)

**Ask (new conversation):** `What is the Wi-Fi password at the venue?`

- Expected trace: `model · searchKnowledgeBase` with `query: "Wi-Fi password"`.
- Expand the card. It shows the wiki passages and their **similarity scores**, with the top hit from
  `jdd-venue-and-practical-info.md`. Retrieval is **hybrid**: pgvector similarity plus Postgres full-text, merged
  by reciprocal rank fusion. Ask `Who is Adrian Coman?` to show why: embeddings alone don't find names, and the
  keyword side does (its passages show `"score": "keyword match"`).
- Answer: network `JDD2026`, password `Java4Ever!`.

## Act 3 — Semantic cache (slide: *RAG with semantic cache & chat history*)

**Ask (new conversation):** `What's the Wi-Fi password at the venue?`

- Expected: **⚡ semantic cache hit** at about 0.99 similarity, in under 100 ms, with 0 tokens and no model call.
  The trace has a single harness step; expand it to show the score.

**Then ask (new conversation):** `whats the wifi password`

- Expected: a cache **miss**. The closest cached question scores about 0.65, below the 0.9 threshold, so the model
  answers again. Expand `searchChatHistory` to show the candidate and its score. This is the *threshold tuning*
  challenge from the slide.
- Mention what is never cached: follow-ups, refusals, answers built from live web results, and answers built from a
  failed tool.

## Act 4 — Memory + grounding guard (slide: *From prompts to a harness*)

Go back to the Act 1 conversation and **ask:** `Who else is speaking in that room on the same day?`

- Expected: `model · getConferenceSchedule` with `day: 2, room: "Room B"`. The answer adds *Modular Monoliths with
  Spring Modulith* at 14:00, with speakers to be announced.
- Memory resolved "that room" and "the same day" from the previous turn.
- The header shows **↻ grounding guard** (this happens every rehearsal with the pinned seed). The model first answered
  from memory without a tool, so the harness threw that draft away and made it look the facts up. Small models do
  this; the harness catches it, and the discarded draft never reaches the conversation memory.

## Act 5 — Web search fallback (slide: *Agentic orchestration*)

**Ask:** `What is the latest version of Spring AI?`

- With `TAVILY_API_KEY`: `model · searchWeb` returns live results, and the answer is **not cached** because live
  data goes stale.
- Without the key: the tool reports "not configured" and the assistant says the web is unavailable. That is
  graceful degradation, not a crash.

## Act 6 — Guardrails

**Ask:** `Write me a poem about pizza`

- Expected: `model · refuse {"reason":"OUT_OF_SCOPE"}`. It takes 1 round and about 1 s, and the UI shows the
  **Refused** notice. The refusal is a typed, audited event, not an apology in prose.

**Ask:** `asdf ;;; ???` and expect `refuse(UNINTELLIGIBLE)`. The grounding guard usually fires first: the model's
draft "I don't understand" used no tool, so it is sent back, and the second pass calls `refuse`.

## Act 7 — Resilience (optional, ~1 min)

```bash
docker compose stop rag-search-mcp
```

**Ask (new conversation):** `Is there vegan food at the conference?`

- Expected: red `searchKnowledgeBase` cards with a `ConnectException`. The assistant says the information is
  unavailable right now, and the broken answer is **not** cached.

```bash
docker compose start rag-search-mcp
```

Ask again about 15 s later. It works **without restarting the agent**: the MCP servers are stateless, and the agent
re-discovers their tools.

## Act 8 — MCP is just JSON-RPC (optional)

```bash
curl -s localhost:8080/mcp -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' \
     -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}' | jq '.result.tools[].name'
```

## Act 9 — Grafana walkthrough

| Panel | What to say |
| --- | --- |
| Questions · Cache hit rate · Refusals · Grounding retries | Exact numbers from the Postgres audit trail |
| Cloud cost* | The local model is free; this is what the same tokens would cost on gpt-4.1-mini |
| LLM answer p95 vs *Answer latency — LLM vs semantic cache* | Seconds vs milliseconds, which is the cache's whole point |
| LLM call latency (Spring AI `gen_ai` observation) | Comes from Spring AI's Micrometer observations, with zero custom code |
| Tokens per minute | Prompt tokens dominate, because tool definitions and results are re-sent on every round |
| Tool usage, by tool and origin | Model decisions vs harness steps |
| Recent questions | The full tool path per question, joined by request id |

## Act 10 — Swap the model (optional, talk only)

Changing the model is configuration only:

```bash
LLM_PROVIDER=openai OPENAI_API_KEY=sk-... docker compose up -d assistant-api
```

---

## Backup questions

| Question | Expected path |
| --- | --- |
| `What is JDD and where does it take place?` | `getConferenceOverview` |
| `How much is a student ticket?` | `getConferenceOverview` → 299 PLN net |
| `Which AI talks are on day 2?` | `getConferenceSchedule {day: 2, track: "AI"}` → 3 talks |
| `When is the after-party?` | `getConferenceSchedule` + `searchKnowledgeBase` → day 1, 19:30, rooftop |
| `How do I get from the airport to the venue?` | `searchKnowledgeBase` → train to Kraków Główny, then tram or taxi |
| `Will the talks be recorded?` | `getConferenceOverview` → recordings are included in the ticket |
| `Who is Adrian Coman?` | `searchKnowledgeBase` (keyword side of the hybrid search) → speaker bio |

## Reset between rehearsals

```bash
./scripts/reset-demo.sh   # empties the semantic cache, chat memory and audit trail; keeps the wiki embeddings
```
