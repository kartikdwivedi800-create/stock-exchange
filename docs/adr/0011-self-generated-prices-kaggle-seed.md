# ADR-0011: Prices are generated inside the exchange; Kaggle data only seeds the simulator

- **Status:** Accepted
- **Date:** 2026-09-24
- **Decider:** Kartik

## Context

The exchange needs moving prices and liquidity. Options are mirroring a live market feed or generating activity with bots.

## Decision

Prices are set only by trades in our matching engine. A market-simulator service runs bot traders (market makers, noise, value, momentum) around a per-symbol fair value, using a GBM model or a replayed historical day. Starting prices, volatility and volume come from Kaggle NIFTY minute data, processed offline into `data/stocks-seed.json`. No external market API is called at runtime.

## Consequences

- No API keys, rate limits or outages from third parties.
- Runs are reproducible (fixed random seed).
- Prices look realistic but are not real NSE prices, and the UI must say so.
- Dataset licences must be checked before committing derived data.

## Alternatives considered

Live feed (Finnhub/Alpaca/NSE scrapers): rejected, conflicts with our own price discovery, adds limits and mostly covers US stocks.
