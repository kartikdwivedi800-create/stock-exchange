# Architecture Decision Records

Each file records one decision: the context, what we decided, the consequences, and the alternatives we rejected. Accepted ADRs are never edited; a new ADR supersedes an old one.

| # | Decision | Status | Date |
|---|---|---|---|
| 0001 | [Record architecture decisions as ADRs](0001-use-adrs.md) | Accepted | 2026-09-24 |
| 0002 | [No service registry — services find each other by fixed DNS names](0002-no-service-registry.md) | Accepted | 2026-09-24 |
| 0003 | [One database per service on a single PostgreSQL server, managed by Flyway](0003-database-per-service-flyway.md) | Accepted | 2026-09-24 |
| 0004 | [Run Kafka in KRaft mode (no Zookeeper)](0004-kafka-kraft.md) | Accepted | 2026-09-24 |
| 0005 | [JWT signed with HS256, issued by user-service and validated at the gateway](0005-jwt-hs256-at-gateway.md) | Accepted | 2026-09-24 |
| 0006 | [Hybrid order flow: synchronous reservation, asynchronous settlement saga](0006-hybrid-order-saga.md) | Accepted | 2026-09-24 |
| 0007 | [Money as BigDecimal / NUMERIC(19,4), share quantities as long / BIGINT](0007-bigdecimal-money.md) | Accepted | 2026-09-24 |
| 0008 | [Use MapStruct for entity ↔ DTO mapping](0008-mapstruct.md) | Accepted | 2026-09-24 |
| 0009 | [Configuration through Spring profiles and environment variables](0009-config-env-vars.md) | Accepted | 2026-09-24 |
| 0010 | [Retire connectivity-test-service](0010-retire-connectivity-test-service.md) | Accepted | 2026-09-24 |
| 0011 | [Prices are generated inside the exchange; Kaggle data only seeds the simulator](0011-self-generated-prices-kaggle-seed.md) | Accepted | 2026-09-24 |
