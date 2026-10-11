-- NX-206: stock_db full schema (docs/01-architecture.md §5.3 and §4.5)
-- V1 created a skeleton `stocks` table that has never held real data (service returns 501).
-- This migration drops it and recreates the complete schema.

-- ─────────────────────────────────────────────────────────────────────────────
-- stocks — master data per listed symbol plus the live market snapshot
-- (last price, day OHLC, volume) updated from trade-events.
-- ─────────────────────────────────────────────────────────────────────────────
DROP TABLE stocks;

CREATE TABLE stocks (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    symbol          VARCHAR(20)    NOT NULL UNIQUE,
    company_name    VARCHAR(255)   NOT NULL,
    exchange        VARCHAR(10)    NOT NULL DEFAULT 'NEX',
    sector          VARCHAR(50),
    tick_size       NUMERIC(10, 4) NOT NULL DEFAULT 0.05
        CHECK (tick_size > 0),
    lot_size        INTEGER        NOT NULL DEFAULT 1
        CHECK (lot_size > 0),
    last_price      NUMERIC(19, 4) CHECK (last_price > 0),
    previous_close  NUMERIC(19, 4) CHECK (previous_close > 0),
    day_open        NUMERIC(19, 4) CHECK (day_open > 0),
    day_high        NUMERIC(19, 4) CHECK (day_high > 0),
    day_low         NUMERIC(19, 4) CHECK (day_low > 0),
    day_volume      BIGINT         NOT NULL DEFAULT 0
        CHECK (day_volume >= 0),
    trading_status  VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE'
        CHECK (trading_status IN ('ACTIVE', 'HALTED', 'DELISTED')),
    version         BIGINT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL
);

-- ─────────────────────────────────────────────────────────────────────────────
-- stock_price_history — append-only tick log, one row per executed trade.
-- Source data for charts and candle (re)building.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE stock_price_history (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    symbol       VARCHAR(20)    NOT NULL REFERENCES stocks (symbol),
    price        NUMERIC(19, 4) NOT NULL
        CHECK (price > 0),
    quantity     BIGINT         NOT NULL
        CHECK (quantity > 0),
    trade_id     BIGINT,
    recorded_at  TIMESTAMPTZ    NOT NULL
);

-- Newest-first queries per symbol are the common access pattern
CREATE INDEX ix_stock_price_history_symbol_recorded
    ON stock_price_history (symbol, recorded_at DESC);

-- ─────────────────────────────────────────────────────────────────────────────
-- candles — OHLCV bars per symbol and timeframe. 1m bars are built from trades;
-- larger timeframes are aggregated from them. Named `timeframe`, not `interval`
-- (INTERVAL is a PostgreSQL type keyword).
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE candles (
    symbol     VARCHAR(20)    NOT NULL REFERENCES stocks (symbol),
    timeframe  VARCHAR(5)     NOT NULL
        CHECK (timeframe IN ('1m', '5m', '15m', '1h', '1d')),
    open_time  TIMESTAMPTZ    NOT NULL,
    open       NUMERIC(19, 4) NOT NULL,
    high       NUMERIC(19, 4) NOT NULL,
    low        NUMERIC(19, 4) NOT NULL,
    close      NUMERIC(19, 4) NOT NULL,
    volume     BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (symbol, timeframe, open_time)
);
