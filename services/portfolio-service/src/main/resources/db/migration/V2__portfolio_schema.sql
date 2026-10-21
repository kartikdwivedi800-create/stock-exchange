-- NX-208: portfolio_db full schema (docs/01-architecture.md 5.6 and 5.8)
-- V1 created a skeleton `portfolios` table that has never held real data (service returns 501).
-- This migration drops it and recreates the complete schema.
-- The notifications table is deliberately NOT here: it moves to NX-1201.

-- ----------------------------------------------------------------------------
-- holdings - one row per (user, symbol); tracks owned and reserved share counts.
-- reserved_quantity is locked by open SELL orders and can never exceed quantity.
-- ----------------------------------------------------------------------------
DROP TABLE portfolios;

CREATE TABLE holdings (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT         NOT NULL,
    symbol            VARCHAR(20)    NOT NULL,
    quantity          BIGINT         NOT NULL DEFAULT 0
        CHECK (quantity >= 0),
    reserved_quantity BIGINT         NOT NULL DEFAULT 0,
    average_buy_price NUMERIC(19, 4) NOT NULL DEFAULT 0
        CHECK (average_buy_price >= 0),
    realized_pnl      NUMERIC(19, 4) NOT NULL DEFAULT 0,
    version           BIGINT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ    NOT NULL,
    updated_at        TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uq_holdings_user_symbol UNIQUE (user_id, symbol),
    CONSTRAINT ck_holdings_reserved_within_quantity
        CHECK (reserved_quantity >= 0 AND reserved_quantity <= quantity)
);

-- ----------------------------------------------------------------------------
-- share_reservations - shares locked against a specific SELL order.
-- Released when the order is cancelled; consumed as trades settle.
-- ----------------------------------------------------------------------------
CREATE TABLE share_reservations (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id          BIGINT      NOT NULL,
    holding_id        BIGINT      NOT NULL REFERENCES holdings (id),
    quantity_reserved BIGINT      NOT NULL
        CHECK (quantity_reserved > 0),
    quantity_used     BIGINT      NOT NULL DEFAULT 0,
    status            VARCHAR(20) NOT NULL
        CHECK (status IN ('ACTIVE', 'RELEASED', 'CONSUMED')),
    version           BIGINT      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_share_reservations_order UNIQUE (order_id),
    CONSTRAINT ck_share_reservations_used_within_reserved
        CHECK (quantity_used >= 0 AND quantity_used <= quantity_reserved)
);

CREATE INDEX ix_share_reservations_holding
    ON share_reservations (holding_id);

-- ----------------------------------------------------------------------------
-- holding_transactions - append-only ledger of trades applied to holdings.
-- UNIQUE (trade_id, user_id, side) stops the same trade being applied twice.
-- realized_pnl is NULL for BUY and set for SELL.
-- ----------------------------------------------------------------------------
CREATE TABLE holding_transactions (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT         NOT NULL,
    symbol       VARCHAR(20)    NOT NULL,
    trade_id     BIGINT         NOT NULL,
    side         VARCHAR(4)     NOT NULL
        CHECK (side IN ('BUY', 'SELL')),
    quantity     BIGINT         NOT NULL
        CHECK (quantity > 0),
    price        NUMERIC(19, 4) NOT NULL
        CHECK (price > 0),
    realized_pnl NUMERIC(19, 4),
    created_at   TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uq_holding_transactions_trade UNIQUE (trade_id, user_id, side)
);

-- "My transactions, newest first"
CREATE INDEX ix_holding_transactions_user_created
    ON holding_transactions (user_id, created_at DESC);

-- ----------------------------------------------------------------------------
-- outbox - transactional outbox for reliable Kafka publishing (ADR-0003 4.3).
-- A poller reads rows where published_at IS NULL and publishes them to Kafka.
-- ----------------------------------------------------------------------------
CREATE TABLE outbox (
    id             UUID         NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   VARCHAR(64)  NOT NULL,
    event_type     VARCHAR(50)  NOT NULL,
    topic          VARCHAR(100) NOT NULL,
    message_key    VARCHAR(100),
    payload        JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    published_at   TIMESTAMPTZ
);

-- Partial index: the poller only ever queries unpublished rows
CREATE INDEX ix_outbox_unpublished
    ON outbox (created_at)
    WHERE published_at IS NULL;

-- ----------------------------------------------------------------------------
-- processed_events - idempotency guard for Kafka consumers.
-- Before processing an incoming event, insert its event_id here in the same
-- transaction; duplicate events (retries, replays) are skipped.
-- ----------------------------------------------------------------------------
CREATE TABLE processed_events (
    event_id     UUID        NOT NULL PRIMARY KEY,
    consumer     VARCHAR(50) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);