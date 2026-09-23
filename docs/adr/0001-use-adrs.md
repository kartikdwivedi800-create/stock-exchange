# ADR-0001: Record architecture decisions as ADRs

- **Status:** Accepted
- **Date:** 2026-09-24
- **Decider:** Kartik

## Context

The project is moving from a Sprint 0 skeleton to a 15-sprint build. Decisions made now (infrastructure, security, data model) will shape every later sprint, and the reasons behind them are easy to forget.

## Decision

Every significant decision is recorded as a short Markdown ADR in `docs/adr/`, numbered in order. An ADR is never edited after it is accepted; a later ADR supersedes it instead.

## Consequences

- Anyone (including future me) can see why things are the way they are.
- Changing a decision means writing a new ADR, which forces the trade-off to be stated.

## Alternatives considered

Keep decisions only in `01-architecture.md`: rejected, because that doc describes the current state and loses the history.
