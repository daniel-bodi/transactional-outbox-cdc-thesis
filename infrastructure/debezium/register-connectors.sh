#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONNECT_URL="${KAFKA_CONNECT_URL:-http://localhost:8083}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-60}"

register() {
  local name=$1
  local config_file=$2

  echo "Registering connector: $name"
  # the connector validates its outbox table on registration, and the services create it on startup,
  # so registration is retried until the services are up
  for attempt in $(seq 1 "$MAX_ATTEMPTS"); do
    if curl -sf -X PUT \
        -H 'Content-Type: application/json' \
        --data @"$config_file" \
        "$CONNECT_URL/connectors/$name/config" > /dev/null; then
      echo "  OK"
      return
    fi
    echo "  not ready yet (attempt $attempt/$MAX_ATTEMPTS)"
    sleep 2
  done
  echo "  FAILED" >&2
  exit 1
}

register "subscription-outbox-connector" "$SCRIPT_DIR/subscription-outbox-connector.json"
register "payment-outbox-connector" "$SCRIPT_DIR/payment-outbox-connector.json"

echo
echo "Registered connectors:"
curl -sf "$CONNECT_URL/connectors"
echo
