#!/usr/bin/env bash
# Loads the models into Ollama's memory before going on stage (the first call after a cold start can
# take ~40 s), then clears the demo state so the warm-up questions don't show up as cache hits.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Warming up the chat and embedding models…"
curl -s -m 300 http://localhost:8090/query -H 'Content-Type: application/json' \
     -d '{"prompt":"How much is a student ticket?"}' > /dev/null
./scripts/reset-demo.sh
echo "Ready."
