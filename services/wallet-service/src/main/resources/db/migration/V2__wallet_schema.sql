-- NX-205: wallet_db full schema (docs/01-architecture.md §5.2 and §5.8)
-- V1 created a skeleton `wallets` table that has never held real data (service returns 501).
-- This migration drops it and recreates the complete schema.

-- ─────────────────────────────────────────────────────────────────────────────
-- wallets — one row per user; tracks available and reserved cash balances.
-- ─────────────────────────────────────────────────────────────────────────────
DROP TABLE wallets;

CREATE TABLE wallets (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT       NOT NULL UNIQUE,
    currency          VARCHAR(3)   NOT NULL DEFAULT 'INR',
    available_balance NUMERIC(19, 4) NOT NULL DEFAULT 0
        CHECK (available_balance >= 0),
    reserved_balance  NUMERIC(19, 4) NOT NULL DEFAULT 0
        CHECK (reserved_balance >= 0),
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL
);

-- ─────────────────────────────────────────────────────────────────────────────
-- wallet_transactions — append-only ledger of every balance change.
-- Each row records the type of movement and the resulting balances.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE wallet_transactions (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    wallet_id        BIGINT        NOT NULL REFERENCES wallets (id),
    type             VARCHAR(20)   NOT NULL
        CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'HOLD', 'RELEASE', 'TRADE_DEBIT', 'TRADE_CREDIT')),
    amount           NUMERIC(19, 4) NOT NULL
        CHECK (amount > 0),
    available_after  NUMERIC(19, 4) NOT NULL,
    reserved_after   NUMERIC(19, 4) NOT NULL,
    reference_type   VARCHAR(20),
    reference_id     VARCHAR(64),
    idempotency_key  VARCHAR(64)   UNIQUE,
    created_at       TIMESTAMPTZ   NOT NULL
);

-- Newest-first queries per wallet are the common access pattern
CREATE INDEX ix_wallet_transactions_wallet_created
    ON wallet_transactions (wallet_id, created_at DESC);

-- ─────────────────────────────────────────────────────────────────────────────
-- fund_reservations — holds reserved cash against a specific BUY order.
-- Released when an order is cancelled; consumed when a trade settles.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE fund_reservations (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id        BIGINT        NOT NULL UNIQUE,
    wallet_id       BIGINT        NOT NULL REFERENCES wallets (id),
    amount_reserved NUMERIC(19, 4) NOT NULL
        CHECK (amount_reserved > 0),
    amount_used     NUMERIC(19, 4) NOT NULL DEFAULT 0
        CHECK (amount_used >= 0 AND amount_used <= amount_reserved),
    status          VARCHAR(20)   NOT NULL
        CHECK (status IN ('ACTIVE', 'RELEASED', 'CONSUMED')),
    version         BIGINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL
);

-- ─────────────────────────────────────────────────────────────────────────────
-- outbox — transactional outbox for reliable Kafka publishing (ADR-0003 §4.3).
-- A poller reads rows where published_at IS NULL and publishes them to Kafka.
-- ─────────────────────────────────────────────────────────────────────────────
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

-- ─────────────────────────────────────────────────────────────────────────────
-- processed_events — idempotency guard for Kafka consumers.
-- Before processing an incoming event, insert its event_id here in the same
-- transaction; duplicate events (retries, replays) are skipped.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE processed_events (
    event_id     UUID        NOT NULL PRIMARY KEY,
    consumer     VARCHAR(50) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);
