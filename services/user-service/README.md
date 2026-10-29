# User Service

Registration, login and user profiles for Nexus Exchange.

## Details
- Port: `8081`
- Language: Java 21
- Framework: Spring Boot 3
- Package: `com.nexusexchange.user`

## Endpoints
- **Register**: `POST http://localhost:8081/api/v1/auth/register`
- **Login**: `POST http://localhost:8081/api/v1/auth/login`
- **Profile**: `GET` / `PUT http://localhost:8081/api/v1/users/me` (header `X-User-Id`)
- **Health Check**: `GET http://localhost:8081/health` and `GET http://localhost:8081/actuator/health`
- **Swagger Documentation**: `GET http://localhost:8081/swagger-ui.html`
- **API Documentation (JSON)**: `GET http://localhost:8081/api-docs`

> Errors use the shared `ErrorResponse` from common-library. Passwords and tokens are still mocks until Sprint 3 (NX-301/302).

## Configuration
Environment variables (defaults in brackets): `DB_HOST` (`localhost`), `DB_PORT` (`5432`), `DB_NAME` (`user_db`), `DB_USERNAME` (`user_service`), `DB_PASSWORD` (`user_service`), `SERVER_PORT` (`8081`). Profile `docker` points the database at host `postgres`.

## Login lockout
After 5 consecutive wrong passwords (`nexus.security.login.max-failed-attempts`) an account is locked for 15 minutes (`lock-duration`) and login answers **423 Locked**, even with the right password. Trade-off we accept: a 423 reveals that the account exists (user enumeration), while unknown usernames get the same 401 as wrong passwords. A successful login resets the counter.

## Tests
`UserServiceApplicationTests` runs against a Testcontainers PostgreSQL and is skipped when Docker is not running. `UserServiceTests` are plain unit tests.
