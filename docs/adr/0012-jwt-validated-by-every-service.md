# ADR-0012: Every service validates JWTs itself, using Spring Security's resource server

- **Status:** Accepted
- **Date:** 2026-10-22
- **Decider:** Kartik
- **Amends:** [ADR-0005](0005-jwt-hs256-at-gateway.md). HS256, the token lifetimes and the gateway check still stand. What changes: services no longer trust `X-User-Id` blindly, and the library is no longer jjwt.

## Context

ADR-0005 planned for the API gateway (Sprint 4) to be the only place that checks JWTs. Services would then trust the `X-User-Id` / `X-User-Role` headers the gateway adds. That leaves two problems:

1. Any request that reaches a service **without going through the gateway** can claim to be any user just by setting a header. Examples: a misconfigured port, another compromised container, a developer calling the service directly.
2. In Sprint 3 there is no gateway yet, so user-service couldn't enforce real authentication until Sprint 4.

## Decision

- **Every service validates the access token itself** as an OAuth2 *resource server*: signature (HS256 shared secret), expiry and issuer. The authenticated user id comes from the token's `sub` claim, never from a request header.
- The **gateway also validates** tokens (Sprint 4), so bad requests are rejected early. That's two layers of defence.
- Use **Spring Security's built-in JWT support** (`spring-boot-starter-oauth2-resource-server`: `JwtEncoder`/`JwtDecoder`, Nimbus JOSE underneath) for both issuing (user-service) and validating (all services). The unused **jjwt** dependency is removed from the root POM.
- From Sprint 4, the validation setup is shared through common-library, so every service gets identical security with one dependency.

## Consequences

- Zero-trust between services: forging `X-User-Id` has no effect.
- Each service needs the JWT secret (`JWT_SECRET`) in its environment. This stays acceptable with HS256 for a single-team project. Moving to RS256 + JWKS (backlog) would mean services only need the **public** key.
- Validation is local: a signature check, with no network call to user-service, so it adds no latency or coupling.
- Slightly more configuration per service. Shared auto-configuration in common-library removes that duplication.

## Alternatives considered

- **Gateway-only validation (ADR-0005 as written):** rejected; services would trust a header anyone can set.
- **jjwt with a hand-written filter:** rejected. Spring's resource server already provides the filter, error handling and test helpers (`jwt()` in MockMvc), so there's less custom security code to get wrong.
- **Calling user-service to introspect every token:** rejected, because it adds a network hop to every request and makes user-service a single point of failure.
