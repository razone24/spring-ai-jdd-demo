#!/usr/bin/env bash
# Clears what the demo accumulates — semantic cache, conversation memory, audit trail — so the next
# run starts cold (first question = cache miss) and the Grafana numbers start from zero.
# The wiki embeddings are kept, so nothing is re-embedded.
set -euo pipefail
cd "$(dirname "$0")/.."

docker compose exec -T postgres psql -U jdd -d jdd -q <<'SQL'
TRUNCATE chat_history;
TRUNCATE query_audit CASCADE;
DELETE FROM spring_ai_chat_memory;
SQL

echo "Demo state cleared: semantic cache, chat memory and audit trail are empty."
