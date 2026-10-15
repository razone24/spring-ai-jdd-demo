#!/usr/bin/env bash
# Clears what the demo accumulates — semantic cache, conversation memory, audit trail, Prometheus
# history — so the next run starts cold (first question = cache miss) and Grafana starts from zero.
# The wiki embeddings are kept, so nothing is re-embedded.
set -euo pipefail
cd "$(dirname "$0")/.."

docker compose exec -T postgres psql -U jdd -d jdd -q <<'SQL'
-- The semantic cache has one table per LLM provider (plus the pre-provider one); clear whichever exist.
DO $$
DECLARE
    cache_table text;
BEGIN
    FOREACH cache_table IN ARRAY ARRAY['chat_history', 'chat_history_openai', 'chat_history_ollama'] LOOP
        IF to_regclass(cache_table) IS NOT NULL THEN
            EXECUTE format('TRUNCATE %I', cache_table);
        END IF;
    END LOOP;
END $$;
TRUNCATE query_audit CASCADE;
DELETE FROM spring_ai_chat_memory;
SQL

# Prometheus keeps its time series inside the container; recreating it empties the live graphs.
docker compose up -d --force-recreate prometheus > /dev/null 2>&1

echo "Demo state cleared: semantic cache, chat memory, audit trail and metrics history are empty."
