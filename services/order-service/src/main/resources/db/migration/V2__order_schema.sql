-- NX-207: order_db full schema (docs/01-architecture.md §5.4 and §5.8)
-- V1 created a skeleton `orders` table that has never held real data (service returns 501).
-- This migration drops it and recreates the complete schema.
-- Matching-engine tables (book_orders, trades) are deliberately NOT here: they move to NX-904.

-- -----------------------------------------------------------------------------
-- orders — one row per order; aggregate root of the order lifecycle.
-- -----------------------------------------------------------------------------
DROP TABLE orders;

CREATE TABLE orders (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT         NOT NULL,
    client_order_id  VARCHAR(64),
    symbol           VARCHAR(20)    NOT NULL,
    side             VARCHAR(4)     NOT NULL
        CHECK (side IN ('BUY', 'SELL')),
    order_type       VARCHAR(10)    NOT NULL
        CHECK (order_type IN ('LIMIT', 'MARKET', 'STOP', 'STOP_LIMIT')),
    price            NUMERIC(19, 4)
        CHECK (price > 0),
    quantity         BIGINT         NOT NULL
        CHECK (quantity > 0),
    filled_quantity  BIGINT         NOT NULL DEFAULT 0
        CHECK (filled_quantity >= 0 AND filled_quantity <= quantity),
    avg_fill_price   NUMERIC(19, 4),
    status           VARCHAR(20)    NOT NULL
        CHECK (status IN ('NEW', 'OPEN', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'REJECTED')),
    rejection_reason VARCHAR(255),
    version          BIGINT         NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ    NOT NULL,
    updated_at       TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uq_orders_user_client_order UNIQUE (user_id, client_order_id),
    CONSTRAINT ck_orders_market_has_no_price
        CHECK ((order_type = 'MARKET' AND price IS NULL)
            OR (order_type <> 'MARKET' AND price IS NOT NULL))
);

-- "My orders, newest first"
CREATE INDEX ix_orders_user_created
    ON orders (user_id, created_at DESC);

-- Open orders per symbol
CREATE INDEX ix_orders_symbol_status
    ON orders (symbol, status);

-- -----------------------------------------------------------------------------
-- order_fills — append-only record of each trade applied to an order.
-- UNIQUE (order_id, trade_id) makes applying the same trade twice impossible.
-- -----------------------------------------------------------------------------
CREATE TABLE order_fills (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id    BIGINT         NOT NULL REFERENCES orders (id),
    trade_id    BIGINT         NOT NULL,
    price       NUMERIC(19, 4) NOT NULL
        CHECK (price > 0),
    quantity    BIGINT         NOT NULL
        CHECK (quantity > 0),
    executed_at TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uq_order_fills_order_trade UNIQUE (order_id, trade_id)
);

-- -----------------------------------------------------------------------------
-- order_status_history — append-only audit trail of order state-machine transitions.
-- from_status is NULL for the first transition.
-- -----------------------------------------------------------------------------
CREATE TABLE order_status_history (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id    BIGINT       NOT NULL REFERENCES orders (id),
    from_status VARCHAR(20)
        CHECK (from_status IN ('NEW', 'OPEN', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'REJECTED')),
    to_status   VARCHAR(20)  NOT NULL
        CHECK (to_status IN ('NEW', 'OPEN', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'REJECTED')),
    reason      VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_order_status_history_order_created
    ON order_status_history (order_id, created_at);

-- -----------------------------------------------------------------------------
-- outbox — transactional outbox for reliable Kafka publishing (ADR-0003 §4.3).
-- A poller reads rows where published_at IS NULL and publishes them to Kafka.
-- -----------------------------------------------------------------------------
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

-- -----------------------------------------------------------------------------
-- processed_events — idempotency guard for Kafka consumers.
-- Before processing an incoming event, insert its event_id here in the same
-- transaction; duplicate events (retries, replays) are skipped.
-- -----------------------------------------------------------------------------
CREATE TABLE processed_events (
    event_id     UUID        NOT NULL PRIMARY KEY,
    consumer     VARCHAR(50) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);
