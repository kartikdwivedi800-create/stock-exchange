-- NX-204: bring `users` up to the target design (docs/01-architecture.md §5.1)
-- and add `refresh_tokens` for Sprint 3 (JWT refresh / logout).

-- 1. Timestamps in UTC: TIMESTAMP (no zone) -> TIMESTAMPTZ. Existing values were written in UTC.
ALTER TABLE users
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- 2. Account status (common enum UserStatus) and optimistic-locking version
ALTER TABLE users
    ADD COLUMN status  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'INACTIVE')),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 3. BCrypt hashes are 60 characters; 100 leaves room for other algorithms
ALTER TABLE users ALTER COLUMN password_hash TYPE VARCHAR(100);

-- 4. Usernames and emails are unique regardless of letter case ("Kartik" = "kartik")
ALTER TABLE users DROP CONSTRAINT users_username_key;
ALTER TABLE users DROP CONSTRAINT users_email_key;
CREATE UNIQUE INDEX ux_users_username_lower ON users (LOWER(username));
CREATE UNIQUE INDEX ux_users_email_lower    ON users (LOWER(email));

-- 5. Refresh tokens: only a hash of the token is stored, never the token itself
CREATE TABLE refresh_tokens (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(100) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
