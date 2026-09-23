# ADR-0003: One database per service on a single PostgreSQL server, managed by Flyway

- **Status:** Accepted
- **Date:** 2026-09-24
- **Decider:** Kartik

## Context

All services currently share one database (`app`) and let Hibernate create tables (`ddl-auto: update`). This couples services through the schema and makes changes unrepeatable.

## Decision

One PostgreSQL 16 server with a separate database per service (`user_db`, `wallet_db`, `order_db`, `portfolio_db`, `stock_db`, `matching_db`, `notification_db`). A service may only connect to its own database. Schemas are versioned with Flyway (`V1__init.sql`, …) and Hibernate runs with `ddl-auto: validate`.

## Consequences

- Service boundaries are enforced: data owned by another service is reached through its API or events.
- Schema changes are reviewed and repeatable across environments.
- Only one Postgres container locally, so it stays light on a laptop.
- Cross-service reports need events or API composition, never SQL joins.

## Alternatives considered

Shared schema: rejected (coupling). One Postgres container per service: rejected (too heavy locally, no real benefit).
