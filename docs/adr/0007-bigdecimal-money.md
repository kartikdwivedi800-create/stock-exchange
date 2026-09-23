# ADR-0007: Money as BigDecimal / NUMERIC(19,4), share quantities as long / BIGINT

- **Status:** Accepted
- **Date:** 2026-09-24
- **Decider:** Kartik

## Context

Entities, DTOs and events currently use `Double` for prices, balances and quantities. Floating point cannot represent most decimal amounts exactly, so balances would drift.

## Decision

All money uses `java.math.BigDecimal` in code and `NUMERIC(19,4)` in the database, compared with `compareTo`, never `equals`. Share quantities are whole numbers: `long` / `BIGINT`. Currency is INR only for now.

## Consequences

- Ledger totals stay exact, which the reconciliation job depends on.
- Slightly more verbose arithmetic; rounding rules must be explicit (`RoundingMode.HALF_EVEN`).

## Alternatives considered

`Double`: rejected. Integer paise (`long`): viable, but BigDecimal is clearer to read and standard in Spring/JPA.
