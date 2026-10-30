# Sprint 3 — Authentication & Security (user-service)

> **Goal:** real, secure identity. A user can register, log in, call protected endpoints with a JWT, refresh and log out; admins are recognised; brute-force logins are blocked.
> **Branch:** `feature/sprint-3` (from `main`). **Who:** Kartik writes the code; Claude reviews every commit (build, tests, security review).
> **Decisions:** [ADR-0012](../adr/0012-jwt-validated-by-every-service.md) (every service validates JWTs, Spring Security resource server). ADR-0005 still applies for HS256, token lifetimes and the gateway's role.

---

## 1. The big picture

### Tokens

| | Access token | Refresh token |
|---|---|---|
| Format | **JWT** signed with HS256 | **Opaque** random string (32 random bytes, Base64URL) — *not* a JWT |
| Lifetime | 15 minutes | 7 days |
| Sent on | Every API call: `Authorization: Bearer <jwt>` | Only to `/auth/refresh` and `/auth/logout` (JSON body) |
| Checked by | Signature + expiry, **no DB lookup** | **DB lookup** of its SHA-256 hash in `refresh_tokens` |
| Revocable | No (short life instead) | Yes (`revoked_at`) |

**Access-token claims**

| Claim | Example | Meaning |
|---|---|---|
| `iss` | `nexus-exchange` | Who issued it; the decoder rejects other issuers |
| `sub` | `"42"` | User id (string, per the JWT spec) |
| `username` | `kartik` | Convenience for logs and the UI |
| `role` | `ROLE_USER` | Becomes the Spring Security authority |
| `iat` / `exp` | epoch seconds | Issued-at and expiry (iat + 15 min) |
| `jti` | UUID | Unique token id (useful for audit and a future deny-list) |

### Endpoints after this sprint (user-service, all under `/api/v1`)

| Method & path | Auth | Story |
|---|---|---|
| `POST /auth/register` | public | existing (BCrypt in NX-301) |
| `POST /auth/login` | public | NX-302 (+ lockout NX-308) |
| `POST /auth/refresh` | public (refresh token in body) | NX-303 |
| `POST /auth/logout` | public (refresh token in body) | NX-303 |
| `GET /users/me` | **Bearer JWT** | NX-305 |
| `PUT /users/me` | **Bearer JWT** | NX-305 |
| `PUT /users/me/password` | **Bearer JWT** | NX-309 |
| `GET /admin/users` | **Bearer JWT + ROLE_ADMIN** | NX-306 |
| `/health`, `/actuator/health/**`, `/swagger-ui/**`, `/api-docs/**` | public | NX-304 |

### Flows

```
REGISTER   POST /auth/register ─► validate ─► BCrypt(password) ─► INSERT users ─► 201 profile

LOGIN      POST /auth/login ─► find user (username OR email, ignore case)
             ├─ not found        ─► run a dummy BCrypt check (same timing) ─► 401 "Invalid credentials"
             ├─ locked_until>now ─► 423 "Account temporarily locked"                       (NX-308)
             ├─ status≠ACTIVE    ─► 403 "Account is not active"
             ├─ wrong password   ─► failed_attempts+1 (lock at 5) ─► 401 "Invalid credentials"
             └─ ok ─► reset failed_attempts ─► access JWT + new refresh token (hash stored) ─► 200

API CALL   GET /users/me  Authorization: Bearer <jwt>
             Spring's BearerTokenAuthenticationFilter ─► verify signature, exp, iss
             ─► JwtAuthenticationToken(sub=42, ROLE_USER) ─► controller reads user id from the token

REFRESH    POST /auth/refresh {refreshToken}
             hash = SHA-256(token) ─► row = find by hash (row lock)
             ├─ not found              ─► 401
             ├─ revoked_at IS NOT NULL ─► REUSE DETECTED ─► revoke ALL of the user's tokens ─► 401
             ├─ expired                ─► 401
             └─ ok ─► revoke this row ─► issue new access JWT + new refresh token ─► 200   (rotation)

LOGOUT     POST /auth/logout {refreshToken} ─► revoke if found ─► always 204
```

### Error responses (all use the common `ErrorResponse` JSON)

| Situation | Status |
|---|---|
| Missing, invalid or expired access token | **401** |
| Valid token but wrong role | **403** |
| Wrong credentials / bad refresh token | **401** (same message for "no such user" and "wrong password") |
| Account locked by too many attempts | **423 Locked** |
| Account SUSPENDED / INACTIVE | **403** |
| Optimistic-lock conflict on update | **409** |

### Configuration (user-service `application.yml`, bound to a `@ConfigurationProperties` record)

```yaml
nexus:
  security:
    jwt:
      issuer: nexus-exchange
      secret: ${JWT_SECRET:dev-only-secret-change-me-at-least-32-bytes-long!!}   # HS256 needs >= 32 bytes
      access-token-ttl: 15m
    refresh-token-ttl: 7d
    login:
      max-failed-attempts: 5
      lock-duration: 15m
```

---

## 2. Suggested order

`NX-301 → NX-304 → NX-302 → NX-305 → NX-303 → NX-306 → NX-308 → NX-309 → NX-307`

Each step leaves the build green. Commit after each story (`feat(NX-30x): …`) and ask for a review before starting the next one.

> **Sprint length:** 9 stories at a learning pace is closer to **1.5–2 weeks** than 1. That's fine; quality over speed.

---

## 3. Stories

Each story has the same layout: **Why** · **Scope** · **Acceptance criteria** · **Hints** (APIs to look up; not full solutions) · **Pitfalls** · **Tests**.

---

### NX-301 — BCrypt password hashing · S

**Why.** The current hash is `"SHA256:" + password.hashCode()`. That is reversible, it collides, and it is not salted. BCrypt is slow on purpose, adds a random salt per password, and has a tunable cost.

**Scope**
- Add `spring-boot-starter-security` to user-service.
- A `PasswordEncoder` bean: `new BCryptPasswordEncoder(12)`.
- `UserServiceImpl.registerUser` stores `passwordEncoder.encode(raw)`. Login uses `passwordEncoder.matches(raw, hash)`.
- Password rules on `UserRegistrationDto`: 8–72 characters. Note: BCrypt only uses the first 72 **bytes**.

**Acceptance criteria**
- Given a new registration, the stored `password_hash` starts with `$2a$12$` or `$2b$12$` and is 60 characters long.
- Given the right password, login succeeds. Given a wrong one, it returns 401.
- Registering the same password twice produces two **different** hashes (random salt).

**Hints.** `org.springframework.security.crypto.password.PasswordEncoder` and `BCryptPasswordEncoder`. Put the bean in a new `config/SecurityConfig` class; you'll grow that class in NX-304.

**Pitfalls**
- Adding the starter **locks every endpoint** behind a generated login page and password. That's expected; NX-304 fixes it. To keep the build green in this commit, add a temporary `SecurityFilterChain` that permits everything, with a `// TODO NX-304` comment.
- Users already in your local `user_db` have old mock hashes and can't log in any more. Re-register, or run `docker exec postgres psql -U user_service -d user_db -c "delete from users"`.

**Tests.** Update `UserServiceTests`: mock the `PasswordEncoder`, or use a real one with cost 4 to keep tests fast. Add a test that a wrong password raises `InvalidCredentialsException`.

---

### NX-304 — Spring Security baseline · M

**Why.** Decide in one place what is public and what needs a token, and make security errors use our JSON error format instead of HTML pages.

**Scope**
- `SecurityFilterChain` bean:
  - CSRF off: safe because we use no cookies or sessions, only bearer tokens.
  - Session policy `STATELESS`.
  - `authorizeHttpRequests` according to the endpoints table above.
  - `oauth2ResourceServer(jwt)` (the decoder bean comes in NX-302; until then you can leave this line out).
- A custom `AuthenticationEntryPoint` (401) and `AccessDeniedHandler` (403) that write the common `ErrorResponse` as JSON.
- `@EnableMethodSecurity`, needed for `@PreAuthorize` in NX-306.

**Acceptance criteria**
- `GET /api/v1/users/me` without a token → 401 with a JSON body (`status`, `message`, `path`, `requestId`), not an HTML page.
- `/api/v1/auth/register`, `/health` and the Swagger UI work without a token.
- No `JSESSIONID` cookie is ever set.

**Hints.** Look up `HttpSecurity` (lambda DSL), `SessionCreationPolicy.STATELESS`, `AuthenticationEntryPoint`, `AccessDeniedHandler`. Inject Spring's `ObjectMapper` to write the JSON.

**Pitfalls**
- Security errors happen in **filters, before** the controller, so our `@RestControllerAdvice` never sees them. That's why the entry point and denied handler must write the response themselves.
- Order of `requestMatchers` matters: the first match wins. Put `anyRequest().authenticated()` last.
- `AuthControllerTest` (`@WebMvcTest`) now loads security. Import your `SecurityConfig` into it and update the expectations.

**Tests.** `@WebMvcTest` tests: no token → 401 JSON; public endpoint → not 401.

---

### NX-302 — JWT access tokens (issue + validate) · M

**Why.** Login must return a signed token that any service can verify without asking user-service.

**Scope**
- Add `spring-boot-starter-oauth2-resource-server`. **Remove the unused `jjwt` entries** from the root POM (ADR-0012).
- A `SecurityProperties` record (`@ConfigurationProperties("nexus.security")`, `@Validated`) with the config shown above.
- Beans: a `JwtEncoder` (`NimbusJwtEncoder` + `ImmutableSecret`) and a `JwtDecoder` (`NimbusJwtDecoder.withSecretKey(...).macAlgorithm(HS256)`) that also validates `iss`.
- A `TokenService` (or `JwtService`) with `String createAccessToken(User user)` that builds the claims table above.
- A `JwtAuthenticationConverter` that turns the `role` claim into a `GrantedAuthority`, with no extra prefix (the value is already `ROLE_USER`).
- Login response: `{ accessToken, tokenType: "Bearer", expiresIn: 900, refreshToken, user }`. For now `refreshToken` is `null`; NX-303 fills it in. Replace the mock `token` field.

**Acceptance criteria**
- Login returns a JWT. Decoding it (for example on jwt.io) shows the claims table, with `exp − iat = 900`.
- That token on `GET /api/v1/users/me` → not 401 (the endpoint is finished in NX-305).
- A token with a changed character, a wrong issuer or an expired `exp` → 401.
- The app **fails to start** if `JWT_SECRET` is shorter than 32 bytes. Use a validation annotation or a check in the bean.

**Hints.** Look up `JwtClaimsSet`, `JwsHeader.with(MacAlgorithm.HS256)`, `JwtEncoderParameters.from(header, claims)`, `NimbusJwtDecoder.withSecretKey(SecretKeySpec)`, `JwtValidators.createDefaultWithIssuer(...)`, `JwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("role")` and `setAuthorityPrefix("")`.

**Pitfalls**
- `NimbusJwtEncoder` assumes **RS256** unless you pass a `JwsHeader` with `HS256`. You'll get a confusing "Failed to select a JWK signing key" error.
- `sub` must be a String: `String.valueOf(user.getId())`.
- Never log the token or the secret.

**Tests**
- Unit test for `TokenService` (claims present, expiry correct).
- Decoder tests: tampered token, wrong issuer and expired token are all rejected. For the expired case, build a token with a clock in the past.

---

### NX-305 — Profile from the token (`/users/me`) · S

**Why.** The user id must come from the **verified token**, never from a header the client can set (that was the `X-User-Id` trust problem).

**Scope**
- `UserController` reads the id from the JWT: `@AuthenticationPrincipal Jwt jwt` → `Long.valueOf(jwt.getSubject())`. Remove `@RequestHeader("X-User-Id")` from user-service.
- `PUT /users/me` takes a new **`UpdateProfileRequest`** record (`firstName`, `lastName` `@NotBlank`; `phoneNumber` optional, pattern for digits and `+`). It can **not** change username, email, role or status.
- Map Spring's `ObjectOptimisticLockingFailureException` → **409** in common-library's `GlobalExceptionHandler`.

**Acceptance criteria**
- With a token for user 42, `GET /users/me` returns user 42. A forged `X-User-Id: 1` header has no effect.
- `PUT /users/me` with a blank first name → 400 with `fieldErrors`. A body containing `"role":"ROLE_ADMIN"` is ignored.

**Hints.** `@AuthenticationPrincipal`, and `SecurityMockMvcRequestPostProcessors.jwt()` for tests, e.g. `.with(jwt().jwt(j -> j.subject("42")))`.

**Pitfalls.** Don't reuse `UserProfileDto` as the request body. That's how "mass assignment" bugs happen: a client sends `role` and becomes admin.

**Tests.** WebMvc tests with `jwt()`; one test proving the `X-User-Id` header is ignored.

---

### NX-303 — Refresh tokens: rotation, reuse detection, logout · L

**Why.** Users stay logged in for days while access tokens stay short-lived, and stolen refresh tokens can be detected and shut down.

**Scope**
- At login, generate the refresh token: 32 bytes from `SecureRandom`, Base64URL without padding. Store **SHA-256(token)** as hex (64 characters) in `refresh_tokens` with `expires_at = now + 7d`. Return the raw token only once, in the response.
- `POST /auth/refresh {refreshToken}` → the rotation logic from the flow above, inside **one transaction**.
- **Reuse detection:** if a revoked token is presented, revoke **all** active refresh tokens of that user and return 401.
- `POST /auth/logout {refreshToken}` → revoke it. Always return 204: logging out twice is not an error, and the response must not reveal whether a token existed.
- Request DTO: `RefreshTokenRequest(@NotBlank String refreshToken)`.

**Acceptance criteria**
- Refresh returns a **new** access token and a **new** refresh token. The old refresh token no longer works.
- Using an old (rotated) refresh token → 401, **and** the newest token of that user stops working too (reuse detection).
- After logout the refresh token → 401. Logging out twice → 204 both times.
- An expired refresh token → 401.

**Hints.** `MessageDigest.getInstance("SHA-256")` and `HexFormat.of().formatHex(...)`, `Base64.getUrlEncoder().withoutPadding()`. Add repository methods such as a `@Modifying` query "revoke all active tokens for user". `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the find-by-hash query stops two parallel refreshes from both succeeding.

**Pitfalls**
- Compare **hashes**, never raw tokens, and never store the raw token.
- Two browser tabs refreshing at the same moment will trigger reuse detection and log the user out. That's an accepted trade-off; mention it in an interview.
- Use `Instant.now()` consistently, or better, inject a `Clock` so tests can control time.

**Tests.** Unit tests for the rotation service with a fixed `Clock`. Cover each branch: not found, revoked (reuse), expired, ok.

---

### NX-306 — Roles & admin seed · S

**Why.** We need at least one admin to manage stocks later (Sprint 6), and proof that role checks work.

**Scope**
- An `AdminSeeder` (`ApplicationRunner`): if `ADMIN_USERNAME` and `ADMIN_PASSWORD` env vars are set and that user doesn't exist, create it with `ROLE_ADMIN`. Idempotent: restarting doesn't create a duplicate. Log `Admin user 'admin' created`, but **never the password**.
- `GET /api/v1/admin/users?page=&size=` → paged list of profiles, protected with `@PreAuthorize("hasRole('ADMIN')")`.

**Acceptance criteria**
- Start with `ADMIN_USERNAME=admin ADMIN_PASSWORD=…` → log in as admin → `/admin/users` returns 200.
- A normal user → 403. No token → 401.
- Without the env vars → no admin is created, and the app still starts.

**Hints.** `ApplicationRunner`, `Pageable` as a controller parameter, and `hasRole('ADMIN')` (this checks for the authority `ROLE_ADMIN`).

**Pitfalls.** Don't seed the admin through a Flyway migration: the hash would have to be committed to git, together with a known password.

---

### NX-308 — Login brute-force lockout · M (new)

**Why.** Without a limit, an attacker can try millions of passwords. After 5 failures, the account locks for 15 minutes.

**Scope**
- **Your first Flyway migration:** `V3__login_lockout.sql` adds `failed_login_attempts INTEGER NOT NULL DEFAULT 0` (with a `CHECK >= 0`) and `locked_until TIMESTAMPTZ` to `users`. Add the fields to `User`.
- Login: if `locked_until > now` → 423 (new `AccountLockedException` mapped in the handler). On a wrong password, increment the counter; at `max-failed-attempts`, set `locked_until = now + lock-duration` and reset the counter. On success, reset the counter and `locked_until`.

**Acceptance criteria**
- 5 wrong passwords → the 6th attempt (even with the **right** password) → 423.
- After `lock-duration` has passed, the right password works again.
- A successful login resets the counter (4 wrong + 1 right + 4 wrong → not locked).

**Hints.** Inject a `Clock` so tests can move time forward without sleeping. Remember: never edit V1/V2. V3 is a new file.

**Pitfalls**
- The failed-attempt update must be **committed even though login fails**. If you throw `InvalidCredentialsException` inside the same `@Transactional` method, the counter update is rolled back too. Look up `noRollbackFor` (or save the counter in a separate transaction).
- Design trade-off: 423 tells an attacker that the account exists (user enumeration). Many real systems accept this; write down your choice in the README.

**Tests.** Unit tests with a mutable or fixed `Clock`, plus a repository test proving the V3 columns and constraint exist.

---

### NX-309 — Change password · S (new)

**Why.** A basic account feature. It also shows how to "log out of all devices".

**Scope**
- `PUT /api/v1/users/me/password` with `ChangePasswordRequest(currentPassword, newPassword)`; `newPassword` uses the same 8–72 rule.
- Check `currentPassword`. Reject a `newPassword` equal to the current one (400). Save the new BCrypt hash. **Revoke all refresh tokens** of the user. Return 204.

**Acceptance criteria**
- Wrong current password → 401 (or 400; choose and document it). Success → 204.
- The old password no longer logs in; the new one does.
- Every refresh token issued before the change → 401.

**Pitfalls.** Existing **access** tokens stay valid for up to 15 minutes. That's the known JWT trade-off; a deny-list on `jti` in Redis is in the backlog.

---

### NX-307 — Auth integration tests · M (last)

**Why.** One test suite that proves the whole flow over real HTTP against a real database. It's the best protection before Sprint 4 changes things.

**Scope.** `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@Import(PostgresTestcontainersConfiguration.class)` + `@Testcontainers(disabledWithoutDocker = true)`, using `TestRestTemplate` or `RestClient`.

**Scenarios (each one a test)**
1. Register → login → `GET /users/me` with the token → 200 and the right user.
2. `GET /users/me` without a token → 401 JSON. With a tampered token → 401.
3. Refresh → new pair. Old refresh token → 401. Then the new one → 401 too (reuse detection).
4. Logout → refresh with that token → 401.
5. 5 wrong passwords → 423.
6. Admin seeded via test properties → `/admin/users` 200. Normal user → 403.
7. Change password → old refresh token → 401; login with the new password → 200.

**Hints.** Override properties in the test with `@TestPropertySource` or `@DynamicPropertySource` (for example `access-token-ttl=2s` to test expiry, plus the admin credentials). Keep the tests independent: unique usernames per test.

**Done when** all scenarios pass locally with Docker Desktop running, and in GitHub CI.

---

## 4. Out of scope (backlog)

RS256 with a JWKS endpoint · access-token deny-list (Redis, by `jti`) · email verification and "forgot password" · OAuth2 login (Google) · refresh tokens in `HttpOnly` cookies for the future React app · scheduled cleanup of expired refresh tokens · reusable security auto-configuration in common-library (**Sprint 4**, when other services need it).

## 5. Learning checklist (good interview questions)

- Why is a JWT not revocable, and how do short access tokens + revocable refresh tokens solve that?
- Why store only a **hash** of the refresh token? Why BCrypt for passwords but SHA-256 for refresh tokens?
  (Passwords are low-entropy, so you need a slow hash. Refresh tokens are 256 random bits, so a fast hash is enough.)
- What is refresh-token **rotation** and **reuse detection**?
- Why is CSRF protection unnecessary here, and when would it be needed again? (With cookies.)
- Why doesn't `@RestControllerAdvice` handle 401/403 from Spring Security?
- What is mass assignment, and how does a dedicated request DTO prevent it?
- Why do failed-login counters need `noRollbackFor`?
- HS256 vs RS256: when would you switch?
