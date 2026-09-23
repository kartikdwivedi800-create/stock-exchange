# ADR-0002: No service registry — services find each other by fixed DNS names

- **Status:** Accepted
- **Date:** 2026-09-24
- **Decider:** Kartik

## Context

Services need to call each other (order → wallet/portfolio reservations) and the gateway needs to route to all of them. Spring Cloud offers Eureka for discovery, but it is another service to run, and both docker-compose and Kubernetes already provide DNS names for services.

## Decision

No Eureka or Consul. Service URLs are configuration (`WALLET_SERVICE_URL=http://wallet-service:8082`) resolved by docker-compose or Kubernetes DNS. Gateway routes use the same names.

## Consequences

- One less moving part to build, run and debug.
- Scaling is handled by Kubernetes Services, not client-side load balancing.
- Moving to Eureka later only needs config changes in the gateway and clients.

## Alternatives considered

Eureka: rejected, adds complexity with no benefit on a single-developer project. Consul: same.
