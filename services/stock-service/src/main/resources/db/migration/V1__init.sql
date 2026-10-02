-- stock-service V1: tables matching the current JPA entities (Sprint 1 skeleton).
-- Flyway runs this once and records it in flyway_schema_history. Never edit an applied
-- migration; add a new V<n>__*.sql instead. Full schemas arrive in later V2+ files.

CREATE TABLE stocks (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    symbol        VARCHAR(20) UNIQUE,
    company_name  VARCHAR(255),
    price         NUMERIC(19, 4)
);
