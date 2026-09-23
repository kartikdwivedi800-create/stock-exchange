# ADR-0009: Configuration through Spring profiles and environment variables

- **Status:** Accepted
- **Date:** 2026-09-24
- **Decider:** Kartik

## Context

Database credentials and hosts are hard-coded in every service's config file.

## Decision

Each service has `application.yml` with placeholders (`${DB_URL}`, `${DB_USERNAME}`, `${JWT_SECRET}`, …) and profiles `local`, `docker` and `k8s`. Local values come from a git-ignored `.env`; `.env.example` is committed. No Spring Cloud Config Server.

## Consequences

- No secrets in git.
- The same image runs in every environment.
- Kubernetes Secrets and ConfigMaps map directly onto the variables.

## Alternatives considered

Spring Cloud Config Server: rejected, another service to run for little gain at this size.
