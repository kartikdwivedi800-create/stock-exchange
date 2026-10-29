-- NX-308: login brute-force lockout.
-- failed_login_attempts counts consecutive wrong passwords; locked_until is set when the limit is reached.

ALTER TABLE users
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN locked_until          TIMESTAMPTZ;

ALTER TABLE users
    ADD CONSTRAINT ck_users_failed_login_attempts CHECK (failed_login_attempts >= 0);
