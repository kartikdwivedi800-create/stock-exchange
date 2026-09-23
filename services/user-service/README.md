# User Service

Registration, login and user profiles for Nexus Exchange.

## Details
- Port: `8081`
- Language: Java 21
- Framework: Spring Boot 3
- Package: `com.nexusexchange.user`

## Endpoints
- **Register**: `POST http://localhost:8081/register`
- **Login**: `POST http://localhost:8081/login`
- **Profile**: `GET` / `PUT http://localhost:8081/profile` (header `X-User-Id`)
- **Health Check**: `GET http://localhost:8081/health` and `GET http://localhost:8081/actuator/health`
- **Swagger Documentation**: `GET http://localhost:8081/swagger-ui.html`
- **API Documentation (JSON)**: `GET http://localhost:8081/api-docs`

> Routes move under `/api/v1` in NX-106. Passwords and tokens are still mocks until Sprint 3 (NX-301/302).

## Configuration
Environment variables (defaults in brackets): `DB_URL` (`jdbc:postgresql://localhost:5432/app`), `DB_USERNAME` (`postgres`), `DB_PASSWORD` (`postgres`), `SERVER_PORT` (`8081`). Profile `docker` points the database at host `postgres`.

## Tests
`UserServiceApplicationTests` runs against a Testcontainers PostgreSQL and is skipped when Docker is not running. `UserServiceTests` are plain unit tests.
