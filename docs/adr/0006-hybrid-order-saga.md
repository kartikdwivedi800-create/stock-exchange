# ADR-0006: Hybrid order flow: synchronous reservation, asynchronous settlement saga

- **Status:** Accepted
- **Date:** 2026-09-24
- **Decider:** Kartik

## Context

An order must never reach the matching engine unless the buyer has the cash or the seller has the shares. Settlement touches wallet, portfolio, order and stock services, which cannot share a transaction.

## Decision

When an order is placed, order-service reserves funds (wallet-service) or shares (portfolio-service) over REST and answers the user immediately with OPEN or REJECTED. Everything after that is event-driven over Kafka: the engine publishes `TradeExecuted`, and each service applies its part of the settlement. Producers use a transactional outbox; consumers are idempotent (`processed_events`); failures go to a dead-letter topic; a permanent failure triggers a compensating `SettlementFailed` flow.

## Consequences

- Users get an instant, definite answer when placing an order.
- The engine never checks balances, so every match can be settled.
- Two synchronous calls need timeouts, retries and a compensating release (Resilience4j).
- The system is eventually consistent after a trade; a nightly reconciliation job checks that cash and shares are conserved.

## Alternatives considered

Fully async reservation via events: rejected for now (the user waits on a pending order with no answer). Orchestrated saga with a coordinator: more moving parts than needed.
