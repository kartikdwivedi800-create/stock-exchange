# Nexus Exchange

A stock exchange simulator built as event-driven Java microservices. Users register, fund a virtual wallet, place BUY/SELL orders, get matched by a price-time-priority matching engine, and see their portfolio and P&L update in real time. Prices are made by trades on the exchange itself, with bot traders (seeded from historical NIFTY data) providing liquidity.

> **Status:** Sprint 1 of 15 complete (foundation). Business logic arrives from Sprint 2 onward. See the [roadmap](docs/03-roadmap-and-sprints.md).

![System architecture](docs/diagrams/01-system-architecture.svg)

## Tech stack

Java 21 · Spring Boot 3.5 · Spring Cloud Gateway · PostgreSQL 16 (one database per service, Flyway) · Apache Kafka (KRaft) · Redis 7 · WebSocket/STOMP · JWT · MapStruct · Testcontainers · Docker / Kubernetes · GitHub Actions

Why each choice was made: [docs/adr/](docs/adr/README.md).

## Services

| Service | Port | Responsibility | Status |
|---|---|---|---|
| [user-service](services/user-service) | 8081 | Registration, login, profiles | Working (mock password hashing and tokens until Sprint 3) |
| [wallet-service](services/wallet-service) | 8082 | Balances, fund reservations, cash settlement | Skeleton (Sprint 5) |
| [order-service](services/order-service) | 8083 | Place, cancel and query orders | Skeleton (Sprint 8) |
| [portfolio-service](services/portfolio-service) | 8084 | Holdings, share reservations, P&L | Skeleton (Sprint 7) |
| [stock-service](services/stock-service) | 8085 | Stock master data, prices, candles, WebSocket | Skeleton (Sprint 6) |
| api-gateway | 8080 | Routing, JWT validation, rate limiting | Planned (Sprint 4) |
| matching-engine | 8086 | Order books, price-time priority matching | Planned (Sprint 9) |
| notification-service | 8087 | User notifications | Planned (Sprint 12) |
| market-simulator | 8088 | Bot traders for liquidity | Planned (Sprints 12–13) |
| [common-library](common-library) | — | Shared events, enums, DTOs, exceptions, error handling | In use by all services |

All REST routes are under `/api/v1`. Each service also exposes `/health`, `/actuator/health`, `/swagger-ui.html` and `/api-docs`.

## Run locally

**Prerequisites:** JDK 21 (the build targets Java 21; newer JDKs compile but tests may behave differently), Docker Desktop.

```bash
# 1. Start infrastructure (PostgreSQL, Kafka, Redis)
docker compose up -d

# 2. Optional: copy the example environment file and adjust values
cp .env.example .env

# 3. Build everything and run the tests
./mvnw clean verify            # Windows PowerShell: .\mvnw clean verify

# 4. Run a service
./mvnw -pl services/user-service spring-boot:run
```

Try it:

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"kartik","email":"kartik@example.com","password":"Secret123!","firstName":"Kartik","lastName":"D"}'
```

Configuration comes from environment variables with local defaults (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, …). See [.env.example](.env.example). The `docker` Spring profile points services at the compose hostnames.

> Tests that need a real database use Testcontainers. They are skipped automatically when Docker is not running.

## Repository layout

```
common-library/          shared code used by every service
services/<name>-service/ one Spring Boot app per service
docs/                    architecture, ADRs, roadmap, diagrams
.github/workflows/       CI (mvn verify on every push and PR)
docker-compose.yml       local infrastructure
```

## Documentation

| Doc | Contents |
|---|---|
| [Architecture](docs/01-architecture.md) | Services, order and settlement flow, matching engine, schemas, Kafka topics, APIs |
| [Progress assessment](docs/02-progress-assessment.md) | What existed before Sprint 1 and what needed to change |
| [Roadmap & sprints](docs/03-roadmap-and-sprints.md) | 15 one-week sprints with stories and acceptance criteria |
| [Master to-do](docs/04-master-todo.md) | Ordered checklist of every story |
| [ADRs](docs/adr/README.md) | Architecture decision records |

## Contributing workflow

- Branch per sprint or story: `feature/<name>`; commit messages `<type>(NX-<story>): <summary>`.
- CI must be green before merging to `main`.
