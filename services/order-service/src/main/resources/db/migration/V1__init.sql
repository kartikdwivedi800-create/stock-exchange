-- order-service V1: tables matching the current JPA entities (Sprint 1 skeleton).
-- Flyway runs this once and records it in flyway_schema_history. Never edit an applied
-- migration; add a new V<n>__*.sql instead. Full schemas arrive in later V2+ files.

CREATE TABLE orders (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    symbol    VARCHAR(20),
    side      VARCHAR(4) CHECK (side IN ('BUY', 'SELL')),
    price     NUMERIC(19, 4),
    quantity  BIGINT
);
