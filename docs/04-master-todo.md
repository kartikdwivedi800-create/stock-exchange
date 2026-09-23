# Nexus Exchange — Master To-Do List (Backend)

> One ordered checklist. Work top to bottom and tick items as they are merged.
> `[x]` = done, `[~]` = partly done, `[ ]` = pending. Story IDs link to [03-roadmap-and-sprints.md](03-roadmap-and-sprints.md).

---

## ✅ Already done (Sprint 0)
- [x] Git repository & project structure (STORY-001)
- [x] Architecture & sprint PDFs (STORY-002 — now superseded by `01-architecture.md`)
- [x] docker-compose with Postgres, Kafka, Zookeeper, Redis (STORY-003)
- [x] Infra connectivity verified via `connectivity-test-service` (STORY-004)
- [x] Maven parent project (STORY-011)
- [x] common-library created: constants, enums, events, exceptions, utils (STORY-010)
- [~] Service skeletons — user, wallet, order, portfolio, stock (STORY-005…009): layers exist, logic stubbed
- [~] User registration & login (STORY-017/018): works with a mock hash and mock JWT
- [~] Health endpoints (STORY-022): 4 of 5 services
- [ ] Carried over from Sprint 0: DB schemas (012–016), Spring Security (019), gateway (020/021), logging (023), CI (024)

---

## 0. Decide (before any code) — Sprint 1
- [x] Review `01-architecture.md` end to end
- [x] Confirm the tech stack table (architecture §2). Decisions to confirm:
  - [x] No Eureka — static DNS names
  - [x] One database per service on a single Postgres server
  - [x] Kafka KRaft (drop Zookeeper)
  - [x] JWT HS256 first (RS256 later)
  - [x] Sync reservation at order placement + async settlement saga
  - [x] MapStruct vs hand-written mappers
  - [x] Retire `connectivity-test-service`
  - [x] Market data: Kaggle-seeded, self-generated prices (no live feed) — architecture §4.6
- [x] Record each decision as an ADR in `docs/adr/` (NX-101)
- [x] Confirm the final Kafka topic list (architecture §6)
- [x] Confirm the order state machine and enums (architecture §4.1)

## 1. Fix the foundation — Sprint 1
- [x] Root `.gitignore` + `.gitattributes`; untrack `target/` and `.idea/`; renormalise line endings (NX-102)
- [x] Delete `untitled/`; remove `connectivity-test-service` (NX-103)
- [x] Root POM `dependencyManagement` / `pluginManagement` + Spring Cloud BOM (NX-104)
- [ ] user-service → `com.nexusexchange.user`, yml, actuator, springdoc, Dockerfile, README (NX-105)
- [ ] Add common-library to every service; shared exception handler; `/api/v1` prefix (NX-106)
- [x] `BigDecimal`/`long` everywhere; `OrderSide`; new `OrderStatus`; richer `BaseEvent` (NX-107)
- [x] Env-var config, `.env.example`, profiles (NX-108)
- [x] GitHub Actions `mvn verify` (NX-109)
- [ ] Rewrite root README

## 2. Design & build the schemas — Sprint 2
- [ ] Kafka KRaft + Kafka UI + topic init job (NX-201)
- [ ] Postgres init script: 7 databases (NX-202)
- [ ] Flyway in every service, `ddl-auto: validate` (NX-203)
- [ ] user_db schema (NX-204)
- [ ] wallet_db schema + entities (NX-205)
- [ ] stock_db schema + seed 20 stocks (NX-206)
- [ ] order_db + matching_db schemas (NX-207)
- [ ] portfolio_db + notification_db schemas (NX-208)
- [ ] Testcontainers base module (NX-209)

## 3. Identity & security — Sprint 3
- [ ] BCrypt (NX-301)
- [ ] JWT access tokens (NX-302)
- [ ] Refresh + logout (NX-303)
- [ ] Spring Security config (NX-304)
- [ ] `/users/me` endpoints (NX-305)
- [ ] Roles + admin seed (NX-306)
- [ ] Auth integration tests (NX-307)

## 4. API gateway & event backbone — Sprint 4
- [ ] api-gateway module + routes (NX-401)
- [ ] JWT filter + header injection (NX-402)
- [ ] Correlation id + JSON logging everywhere (NX-403)
- [ ] CORS + Redis rate limiting (NX-404)
- [ ] Aggregated Swagger (NX-405)
- [ ] Outbox publisher in common (NX-406)
- [ ] `UserCreated` event (NX-407)
- [ ] Idempotent consumer + DLT in common (NX-408)

## 5. Wallet — Sprint 5
- [ ] Auto-create wallet on `UserCreated` (NX-501)
- [ ] `GET /wallet` (NX-502)
- [ ] Deposit / withdraw with idempotency + ledger (NX-503)
- [ ] Transaction history (NX-504)
- [ ] Internal fund reservation API (NX-505)
- [ ] Concurrency tests (NX-506)
- [ ] `WalletUpdated` events (NX-507)

## 6. Stock / market data — Sprint 6
- [ ] List/search/get stocks (NX-601)
- [ ] Admin stock management + halt (NX-602)
- [ ] Redis price cache (NX-603)
- [ ] Price history API (NX-604)
- [ ] Internal quote API (NX-605)
- [ ] Day rollover job (NX-606)
- [ ] Market movers (NX-607)
- [ ] Seed-data pipeline from Kaggle NIFTY data (NX-608)

## 7. Portfolio — Sprint 7
- [ ] Holdings with LTP & P&L (NX-701)
- [ ] Summary (NX-702)
- [ ] Internal share reservation API (NX-703)
- [ ] Avg price & realised P&L domain logic (NX-704)
- [ ] Admin seed holdings (NX-705)
- [ ] Concurrency tests (NX-706)

## 8. Orders — Sprint 8
- [ ] Order validation rules (NX-801)
- [ ] Place order + reservation + idempotency (NX-802)
- [ ] `PlaceOrder` command via outbox (NX-803)
- [ ] Query orders (NX-804)
- [ ] Cancel order (NX-805)
- [ ] State machine + history (NX-806)
- [ ] Failure handling + WireMock tests (NX-807)

## 9. Matching engine — Sprint 9
- [ ] `matching-core` order book (NX-901)
- [ ] 30+ matching scenario tests (NX-902)
- [ ] matching-engine service + Kafka consumer (NX-903)
- [ ] Persist book & trades transactionally (NX-904)
- [ ] Emit trade/order events (NX-905)
- [ ] Restart recovery test (NX-906)
- [ ] Order book snapshot API + events (NX-907)
- [ ] JMH benchmark (NX-908)
- [ ] LOBSTER replay test (NX-909)

## 10. Settlement saga — Sprint 10
- [ ] Wallet settlement (NX-1001)
- [ ] Portfolio settlement (NX-1002)
- [ ] Order fill updates (NX-1003)
- [ ] Release on cancel/reject (NX-1004)
- [ ] Price update from trades (NX-1005)
- [ ] Compensation flow (NX-1006)
- [ ] 🎯 End-to-end trade test (NX-1007) — **backend MVP milestone**
- [ ] Reconciliation job (NX-1008)

## 11. Real time — Sprint 11
- [ ] STOMP `/ws` + price topics (NX-1101)
- [ ] Order book stream (NX-1102)
- [ ] Gateway WS proxy + auth (NX-1103)
- [ ] Candles (NX-1104)
- [ ] Multi-instance broadcast (NX-1105)
- [ ] WS test client (NX-1106)

## 12. Notifications & simulator foundation — Sprint 12
- [ ] notification-service (NX-1201)
- [ ] Notification APIs (NX-1202)
- [ ] Per-user WebSocket push (NX-1203)
- [ ] market-simulator module + bot accounts (NX-1204)
- [ ] Bot trading client (NX-1205)
- [ ] Email channel stub (NX-1206)

## 13. Market simulator agents — Sprint 13
- [ ] Fair-value engine, GBM (NX-1301)
- [ ] Market-maker agent (NX-1302)
- [ ] Noise-trader agents (NX-1303)
- [ ] Value & momentum agents (NX-1304)
- [ ] Historical replay mode (NX-1305)
- [ ] Scenarios & admin controls (NX-1306)
- [ ] Guards & metrics (NX-1307)

## 14. Reliability & observability — Sprint 14
- [ ] Prometheus metrics (NX-1401)
- [ ] Grafana dashboards (NX-1402)
- [ ] OpenTelemetry tracing (NX-1403)
- [ ] DLT admin (NX-1404)
- [ ] Resilience review (NX-1405)
- [ ] k6 load test + report (NX-1406)
- [ ] Chaos checks (NX-1407)

## 15. CI/CD & Kubernetes — Sprint 15
- [ ] Production images (NX-1501)
- [ ] Full CI pipeline + GHCR (NX-1502)
- [ ] Full-stack compose profile (NX-1503)
- [ ] Kubernetes manifests on kind (NX-1504)
- [ ] Deploy script / CD (NX-1505)
- [ ] Final docs & demo script (NX-1506)

## 16. Later — Frontend (not planned yet)
- [ ] React app: auth, dashboard, watchlist, order ticket, order book, portfolio, notifications
