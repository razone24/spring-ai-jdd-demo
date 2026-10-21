#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Warming up the chat and embedding models…"
curl -s -m 300 http://localhost:8090/query -H 'Content-Type: application/json' \
     -d '{"prompt":"How much is a student ticket?"}' > /dev/null
./scripts/reset-demo.sh
echo "Ready."
