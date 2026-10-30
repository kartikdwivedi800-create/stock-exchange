# User Service

Registration, login and user profiles for Nexus Exchange.

## Details
- Port: `8081`
- Language: Java 21
- Framework: Spring Boot 3
- Package: `com.nexusexchange.user`

## Endpoints
- **Register**: `POST /api/v1/auth/register` (public, body `UserRegistrationDto`, returns 201 with `UserProfileDto`)
- **Login**: `POST /api/v1/auth/login` (public, body `UserLoginDto`, returns 200 with JWT access token + refresh token)
- **Refresh**: `POST /api/v1/auth/refresh` (public, body `{"refreshToken": "..."}`, rotates refresh token and returns new token pair)
- **Logout**: `POST /api/v1/auth/logout` (public, body `{"refreshToken": "..."}`, revokes refresh token, always returns 204)
- **Profile**: `GET` / `PUT /api/v1/users/me` (`Authorization: Bearer <access token>`; user id is extracted from token claims)
- **Change password**: `PUT /api/v1/users/me/password` (Bearer token, body `{"currentPassword": "...", "newPassword": "..."}`, revokes all refresh tokens, returns 204)
- **Admin User List**: `GET /api/v1/admin/users?page=&size=` (Bearer token with `ROLE_ADMIN`, paged profile listing)
- **Health Check**: `GET /health` and `GET /actuator/health`
- **Swagger Documentation**: `GET /swagger-ui.html` and `GET /api-docs`

> Errors use the shared `ErrorResponse` from common-library with standard JSON bodies (401 Unauthorized, 403 Forbidden, 423 Locked, etc.).

## Configuration
Environment variables (defaults in brackets):
- `JWT_SECRET`: HMAC-SHA256 secret key for signing and validating JWT access tokens (`dev-only-secret-change-me-at-least-32-bytes-long!!`, must be >= 32 bytes).
- `ADMIN_USERNAME`: Initial admin username seeded on startup by `AdminSeeder`. If empty, admin seeding is skipped.
- `ADMIN_PASSWORD`: Initial admin password seeded on startup. Must be set together with `ADMIN_USERNAME`.
- `DB_HOST`: PostgreSQL database host (`localhost`).
- `DB_PORT`: PostgreSQL database port (`5432`).
- `DB_NAME`: Database name (`user_db`).
- `DB_USERNAME`: Database user (`user_service`).
- `DB_PASSWORD`: Database password (`user_service`).
- `SERVER_PORT`: HTTP server port (`8081`). Profile `docker` points the database at host `postgres`.

## Changing the password
`PUT /api/v1/users/me/password` with `{"currentPassword", "newPassword"}` (new password 8-72 characters, different from the current one). A wrong current password answers 401. On success all of the user's refresh tokens are revoked (log out of all devices). Note: **access tokens that were already issued stay valid for up to 15 minutes**, because they are verified by signature and expiry only. This is the known JWT trade-off; a `jti` deny-list is in the backlog.

## Login lockout
After 5 consecutive wrong passwords (`nexus.security.login.max-failed-attempts`) an account is locked for 15 minutes (`lock-duration`) and login answers **423 Locked**, even with the right password. Trade-off we accept: a 423 reveals that the account exists (user enumeration), while unknown usernames get the same 401 as wrong passwords. A successful login resets the counter.

## Tests
- `UserServiceApplicationTests`: Context load against Testcontainers PostgreSQL (skipped when Docker is unavailable).
- `AuthFlowIntegrationTest`: End-to-end integration test scenarios covering registration, login, JWT authentication, token rotation, reuse detection, logout, brute-force lockout, admin authorization, and password change.
- `UserServiceTests`, `RefreshTokenServiceTest`, `TokenServiceTest`, `LoginLockoutTest`: Plain unit and service tests.
