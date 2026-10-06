#!/usr/bin/env bash
# Creates the application topics explicitly; the broker does not auto-create topics.
set -euo pipefail

BOOTSTRAP_SERVER="${BOOTSTRAP_SERVER:-kafka:9092}"
PARTITIONS="${PARTITIONS:-3}"

TOPICS=(
  outbox.event.payment_requested
  outbox.event.payment_succeeded
  outbox.event.payment_failed
)

for topic in "${TOPICS[@]}"; do
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP_SERVER" \
    --create --if-not-exists --topic "$topic" \
    --partitions "$PARTITIONS" --replication-factor 1
done
