# ADR-0004: Run Kafka in KRaft mode (no Zookeeper)

- **Status:** Accepted
- **Date:** 2026-09-24
- **Decider:** Kartik

## Context

`docker-compose.yml` runs Kafka with Zookeeper. Current Kafka versions manage metadata themselves (KRaft), and Zookeeper mode is deprecated.

## Decision

Run a single Kafka broker in KRaft mode locally, plus Kafka UI for debugging. Topics are created explicitly by an init job with fixed partition counts (6 for symbol-keyed topics, 3 for others).

## Consequences

- One container fewer; matches how Kafka is run today.
- Explicit topic creation avoids surprises from auto-created topics with one partition.

## Alternatives considered

Keep Zookeeper: rejected (deprecated). Redpanda: a good Kafka-compatible option, but real Kafka is more valuable to learn here.
