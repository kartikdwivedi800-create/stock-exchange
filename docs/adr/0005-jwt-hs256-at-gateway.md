# ADR-0005: JWT signed with HS256, issued by user-service and validated at the gateway

- **Status:** Accepted
- **Date:** 2026-09-24
- **Decider:** Kartik

## Context

Login currently returns a mock token. We need stateless authentication that the gateway can check without calling user-service on every request.

## Decision

user-service issues a 15-minute access token (HS256, claims `sub`=userId, `role`, `jti`) and a 7-day refresh token (stored hashed, rotated on use, revocable). The signing secret comes from the `JWT_SECRET` environment variable. The gateway validates tokens, strips any client-sent `X-User-*` headers and injects `X-User-Id` and `X-User-Role`. Passwords are hashed with BCrypt (strength 12).

## Consequences

- Simple to implement and test.
- The secret must be shared between user-service and the gateway; it never appears in code or git.
- Downstream services trust `X-User-*` headers, so they must only be reachable through the internal network.

## Alternatives considered

RS256 with a JWKS endpoint: better (no shared secret), deferred to the backlog. Opaque tokens with a session store: rejected, needs a lookup per request.
