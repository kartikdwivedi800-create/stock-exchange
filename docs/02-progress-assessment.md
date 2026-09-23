# Nexus Exchange — Progress Assessment

> Snapshot of the repo as of **2026-09-24** (commit `d0f47d4`, "base classes created EPIC-3 done"), compared against the target in [01-architecture.md](01-architecture.md).

## 1. Summary

| Area | Status | Notes |
|---|---|---|
| Repo & Maven parent | 🟢 Done | Parent POM, 7 modules, Spring Boot 3.5.5, Java 21 |
| Local infra (docker-compose) | 🟢 Done | Postgres 16, Kafka + Zookeeper, Redis 7 |
| Service skeletons | 🟡 Partial | 5 services have their layers, but the logic is stubbed (`return null`) |
| Common library | 🟡 Partial | Good contents, but **no service depends on it** |
| User registration / login | 🟡 Partial | Works, but with a fake hash and fake JWT |
| Health endpoints | 🟡 Partial | 4 services have them; user-service is missing the actuator dependency |
| DB schemas | 🔴 Not started | Hibernate `ddl-auto: update`, shared `app` DB, placeholder entities |
| Spring Security / JWT | 🔴 Not started | |
| API gateway | 🔴 Not started | |
| Kafka producers/consumers | 🔴 Not started | Event classes exist only |
| Matching engine | 🔴 Not started | |
| Notification service | 🔴 Not started | |
| WebSockets / real-time | 🔴 Not started | |
| Logging, metrics, tracing | 🔴 Not started | |
| CI/CD | 🔴 Not started | |
| Tests | 🟡 Minimal | 4 unit tests in user-service; nothing elsewhere |

Out of the Sprint 0 plan (24 stories), about **9 are done or partly done**: 001, 003, 004, 005–009 (skeletons), 010, 011, 017, 018, 022. The remaining 15 carry forward into the new roadmap.

---

## 2. What is already done (keep)

1. **Root `pom.xml`** as a parent, with modules for common-library and all 6 services.
2. **`docker-compose.yml`** with Postgres (with healthcheck), Kafka, Zookeeper and Redis (with healthcheck), plus named volumes.
3. **common-library**:
   - constants: `ApiConstants`, `HeaderConstants`, `KafkaTopics`, `SecurityConstants`
   - `BaseResponse`, `ErrorResponse`
   - enums: `OrderStatus`, `OrderType`, `TransactionType`, `EventType`, `UserStatus`, `NotificationType`
   - 7 events built on `BaseEvent` with `@SuperBuilder`
   - 7 exceptions and 5 utility classes
4. **wallet, order, portfolio and stock services**: consistent layering (controller, service interface + impl, repository, entity, DTOs, mapper), `HealthController`, OpenAPI config, `GlobalExceptionHandler`, Dockerfile, README, `application.yml`, ports 8082–8085.
5. **user-service**:
   - `/register`, `/login` and `GET`/`PUT /profile` (using the `X-User-Id` header)
   - User entity with unique constraints and audit timestamps
   - Validation and custom exceptions
   - 4 service unit tests
6. **connectivity-test-service** proved that Postgres, Redis and Kafka are reachable (the Sprint 0 goal).
7. **Docs**: the architecture and Sprint 0 PDFs.

---

## 3. What needs to change

### 3.1 Repository hygiene (do first — cheap, avoids pain later)

| # | Problem | Fix |
|---|---|---|
| H1 | **37 `target/` build files and 6 `.idea/` files are committed.** There is no root `.gitignore`. | Add a root `.gitignore` (target/, .idea/, *.iml, out/), then `git rm -r --cached` those paths |
| H2 | Many files show as "modified" only because of **CRLF/LF line-ending** differences (Windows) | Add `.gitattributes` (`* text=auto eol=lf`, `*.cmd eol=crlf`) and renormalise once |
| H3 | Stray IntelliJ project `services/connectivity-test-service/untitled/` | Delete |
| H4 | `connectivity-test-service` occupies port **8080** (reserved for the gateway), and its package name is off-standard | Retire the module; move its checks into Actuator health indicators |
| H5 | Credentials (`postgres/postgres`) hard-coded in every config | Use `${DB_USERNAME}` / `${DB_PASSWORD}` with an `.env` file for compose, and commit `.env.example` |
| H6 | README is two lines | Rewrite it: overview, architecture link, how to run, service/port table |

### 3.2 Consistency across services

| # | Problem | Fix |
|---|---|---|
| C1 | user-service uses the package `com.nexus.user_service`, `.properties` config, no actuator, no springdoc and no Dockerfile | Move to `com.nexusexchange.user`, switch to `application.yml`, add actuator + springdoc + Dockerfile + README |
| C2 | **common-library is not a dependency of any service**. Each service duplicates `ErrorResponse` and its own exception handler. | Add the dependency everywhere. Move one `GlobalExceptionHandler` into common as auto-config and delete the duplicates. |
| C3 | Dependency versions (springdoc 2.8.5, lombok) repeated in each POM | Move to root `<dependencyManagement>`/`<pluginManagement>`, and import the Spring Cloud BOM |
| C4 | Controllers have no `/api/v1` prefix even though `ApiConstants.API_V1` exists | Apply the prefix; routes then follow the architecture doc (§7) |
| C5 | Stub `ServiceImpl`s return `null`, so endpoints respond `200` with an empty body | Implement each one in its sprint. Until then, throw `UnsupportedOperationException` (→ 501). |

### 3.3 Domain model corrections

| # | Problem | Fix |
|---|---|---|
| D1 | **`Double` is used for prices, balances and quantities** (entities, DTOs and all events) | `BigDecimal` for money, `long` for shares |
| D2 | `Order` has no `userId`, `status`, `orderType`, `filledQuantity`, `version`; `side` is a String | Redesign per architecture §5.4 and add an `OrderSide` enum |
| D3 | `Wallet` has no `userId` or reserved balance | Redesign per §5.2: available/reserved balances, ledger, reservations |
| D4 | `Stock` uses an `id` lookup; no OHLC, tick size or status | Redesign per §5.3; look stocks up by `symbol` |
| D5 | `Portfolio` has no reserved quantity, realised P&L, unique (user, symbol) or version | Redesign per §5.6 as `holdings` |
| D6 | `OrderStatus` in code (`PENDING, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED`) doesn't match the PDF (`OPEN, MATCHED, …`) | Adopt `NEW, OPEN, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED` |
| D7 | `OrderType` includes `STOP`/`STOP_LIMIT` | Keep them in the enum but reject them in validation for the MVP; only LIMIT and MARKET are supported |
| D8 | `BaseEvent.eventType` is a String; there is no correlation id | Use the `EventType` enum and add `correlationId` and `occurredAt` (UTC `Instant`) |

### 3.4 Data and infrastructure

| # | Problem | Fix |
|---|---|---|
| I1 | All services share one DB (`app`) with `ddl-auto: update` | One database per service, created by an init script; Flyway migrations; `ddl-auto: validate` |
| I2 | Kafka uses Zookeeper | Switch to KRaft mode; add Kafka UI for debugging |
| I3 | Topic names differ between code (`stock-events`) and the PDF (`price-events`), and there are no command topics | Adopt the topic list in architecture §6 and update `KafkaTopics` |
| I4 | Dockerfiles copy a pre-built jar and hard-code EXPOSE | Multi-stage build (or Jib), non-root user, healthcheck |
| I5 | No observability stack | Add Prometheus + Grafana (+ Tempo) to a compose profile |

### 3.5 Security (user-service)

| # | Problem | Fix |
|---|---|---|
| S1 | Password "hash" is `"SHA256:" + password.hashCode()` — reversible and collision-prone | BCrypt via Spring Security `PasswordEncoder` |
| S2 | JWT is `"mock-jwt-token-for-<user>-<uuid>"` | Real signed JWT (access + refresh tokens) |
| S3 | `X-User-Id` header is trusted from any caller | The gateway strips and re-injects it; services are only reachable on the internal network |
| S4 | No role-based access | `ROLE_USER` / `ROLE_ADMIN` enforced at the gateway and with `@PreAuthorize` |

---

## 4. Missing entirely (new work)

- **api-gateway**, **matching-engine**, **notification-service**, **market-simulator** modules
- Kafka producers/consumers, outbox, idempotent consumers, DLT
- Reservation APIs (wallet, portfolio) and the order saga
- Market data: price updates from trades, history, candles, Redis cache, WebSocket broadcast
- Integration tests (Testcontainers), contract/API tests, load tests (k6)
- Structured logging, metrics, tracing
- GitHub Actions CI, image publishing, Kubernetes manifests
