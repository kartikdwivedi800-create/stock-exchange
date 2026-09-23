# ADR-0008: Use MapStruct for entity ↔ DTO mapping

- **Status:** Accepted
- **Date:** 2026-09-24
- **Decider:** Kartik

## Context

Each service has hand-written mapper classes. As entities grow (orders, holdings, wallets), these become long and easy to get wrong.

## Decision

Use MapStruct (annotation processor, configured with Lombok in the root POM). DTOs are Java records. Entities never leave the service layer.

## Consequences

- Mapping code is generated at compile time and fails the build when a field is unmapped (`unmappedTargetPolicy = ERROR`).
- One more annotation processor to configure alongside Lombok.

## Alternatives considered

Hand-written mappers: rejected, too much boilerplate. ModelMapper: rejected, reflection-based and fails at runtime instead of compile time.
