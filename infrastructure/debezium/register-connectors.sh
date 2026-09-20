#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONNECT_URL="${KAFKA_CONNECT_URL:-http://localhost:8083}"

register() {
  local name=$1
  local config_file=$2

  echo "Registering connector: $name"
  curl -sf -X PUT \
    -H 'Content-Type: application/json' \
    --data @"$config_file" \
    "$CONNECT_URL/connectors/$name/config" > /dev/null
  echo "  OK"
}

register "subscription-outbox-connector" "$SCRIPT_DIR/subscription-outbox-connector.json"

echo
echo "Registered connectors:"
curl -sf "$CONNECT_URL/connectors"
echo
