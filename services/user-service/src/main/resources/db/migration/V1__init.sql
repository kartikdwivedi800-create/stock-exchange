-- user-service V1: tables matching the current JPA entities (Sprint 1 skeleton).
-- Flyway runs this once and records it in flyway_schema_history. Never edit an applied
-- migration; add a new V<n>__*.sql instead. Full schemas arrive in later V2+ files.

CREATE TABLE users (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    first_name     VARCHAR(255) NOT NULL,
    last_name      VARCHAR(255) NOT NULL,
    phone_number   VARCHAR(255),
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN')),
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL
);
