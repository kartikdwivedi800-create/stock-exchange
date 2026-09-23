# ADR-0010: Retire connectivity-test-service

- **Status:** Accepted
- **Date:** 2026-09-24
- **Decider:** Kartik

## Context

`connectivity-test-service` proved in Sprint 0 that Postgres, Redis and Kafka are reachable. It uses port 8080 (needed by the gateway), has a non-standard package name and contains a stray IntelliJ project (`untitled/`).

## Decision

Remove the module and folder. Each service instead reports its own dependencies through Actuator health indicators (`/actuator/health`), which Kubernetes readiness probes will use.

## Consequences

- Port 8080 is free for the API gateway.
- Connectivity is checked where it matters: in each real service.

## Alternatives considered

Keep it as a smoke-test tool: rejected, Actuator health covers the same need.
