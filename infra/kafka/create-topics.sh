#!/bin/bash
# Creates every Nexus Exchange topic (and its dead-letter topic) once Kafka is up.
# Run by the kafka-init service in docker-compose.yml. Safe to re-run: --if-not-exists.
# Topic list and partition counts: docs/01-architecture.md §6 and ADR-0004.
set -euo pipefail

BOOTSTRAP="${BOOTSTRAP_SERVERS:-kafka:9092}"
KT=/opt/kafka/bin/kafka-topics.sh

# name:partitions — 6 for symbol-keyed topics (parallelism per symbol), 3 for the rest
TOPICS=(
  "order-commands:6"
  "order-events:6"
  "trade-events:6"
  "price-events:6"
  "user-events:3"
  "wallet-events:3"
  "settlement-events:3"
  "notification-events:3"
)

for entry in "${TOPICS[@]}"; do
  name="${entry%%:*}"
  partitions="${entry##*:}"
  for topic in "$name" "$name.DLT"; do
    $KT --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
        --topic "$topic" --partitions "$partitions" --replication-factor 1
  done
done

echo "Topics now on the broker:"
$KT --bootstrap-server "$BOOTSTRAP" --list
