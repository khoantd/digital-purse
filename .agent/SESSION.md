# Agent session

> Cross-tool handoff state for Cursor, Claude Code, and Kiro. Update at session end (`/handoff`) or phase changes; read at session start (`/resume`).

## Meta

| Field | Value |
|-------|-------|
| **Updated** | 2026-09-30 |
| **Phase** | build |
| **Tool** | cursor |
| **Persona** | _(none)_ |

## Goal

Work through `tasks/security-backlog.md` critical/high/medium items.

## Done

- First-run `/understand` — wrote `.agent/PROJECT.md` and `.agent/onboarding.complete`
- **SEC-01** — signup no longer accepts client roles; always `ROLE_USER` (`SignupRequestMapper` + `SignupRequestMapperTest`)
- **SEC-02** — ownership checks via `SecurityAccess`; wallet/transaction IDOR closed (`ForbiddenException` → 403)
- **SEC-03** — transfer/withdraw/addFunds require wallet ownership before balance mutation
- **SEC-04** (partial) — `.env.properties` gitignored + untracked; `.env.example` added; local JWT rotated (history purge deferred)
- **SEC-05** — `@Positive` + `@Digits` on amount/balance (`AmountValidationTest`)
- Unblocked compile: fixed broken Jackson 3 `AppConfig` (`JsonFormat` → `com.fasterxml.jackson.annotation`)
- **SEC-06** — mass assignment closed (server fields off DTOs; wallet owner from principal; update name/iban only)
- **SEC-07** — access token in memory + HttpOnly refresh cookie + logout denylist (`TokenDenylist`, `/auth/refresh`, `/auth/logout`)
- **SEC-08** — Bucket4j 5/min/IP on auth endpoints (`AuthRateLimiter` + 429)
- **SEC-09** — generic credentials-in-use / unauthorized messages (no username/email enumeration)
- **SEC-10** — `@Version` + pessimistic lock on debit wallet (`V6__wallet_version.sql`, `findByIbanForUpdate`)
- **SEC-11** — prod-safe errors: no stack/`?trace=true` leak; correlationId; `application-prod.yml`
- **SEC-12** — security headers (nosniff, frame DENY, HSTS, CSP)
- **SEC-13** — signup min 12 + common-password denylist; login no longer trims password
- **SEC-14** — INFO logs use entity IDs only (no IBAN/balance/username)
- **SEC-15** — CORS origins from `cors_allowed_origins`; enumerated allowed headers
- Package rename: `com.github.yildizmy` → `com.ros.ewallet` (Maven groupId, source trees, logging keys, docs); clone URL → `github.com/khoantd/e-wallet`; `mvn test` 81/81 green

## In progress

- _(idle)_
- **Blockers:** SEC-04 history purge + DB password rotation need explicit user approval (public GitHub repo)

## Next

1. Complete SEC-04: rotate DB password; `git filter-repo` + force-push if approved
2. Prod: set `app.security.cookieSecure=true` and `cors_allowed_origins` to the real frontend origin; activate `prod` Spring profile
3. Optional: HaveIBeenPwned (or similar) live breach API for signup beyond the local denylist

## Decisions

- Dev: Docker Compose runs Postgres only; backend/frontend run locally
- Prod: `docker-compose.prod.yml` runs frontend + backend + DB
- Base package / groupId is `com.ros.ewallet` (not `com.github.yildizmy`); artifactId remains `e-wallet`
- SEC-01: no new admin role-assignment API in this slice; admin roles stay seed/DB until a secured endpoint exists
- Ownership: admin bypasses; non-admin list endpoints scoped to caller; addFunds requires ownership of `toWallet`
- SEC-04: do not rewrite git history without explicit user OK
- SEC-07: refresh cookie path `/api/v1/auth`; access JWT 15m; refresh 7d; denylist is in-memory (single-instance)
- SEC-10: pessimistic lock on debit + `@Version` defense-in-depth (no retry loop)
- SEC-13: login keeps weak max-only length so seed users can still sign in; signup enforces min 12
- SEC-14: transaction table is the append-only money audit; INFO logs stay ID-only

## Gotchas

- Open `backend/` as the IDE project root for Spring Boot (not monorepo root)
- Env vars in root `.env.properties` (imported by Spring via `application.yml`); copy from `.env.example`
- Enable Lombok annotation processing in IDE
- JaCoCo agent may warn on Java classfile major 69; tests still pass
- Mockito needs agent attach (run tests outside restricted sandbox / with full JVM permissions)
- After JWT rotation, existing tokens are invalid — re-login required
- Flyway `V6__wallet_version.sql` must run before app start after pull
- Local auth cookies: `cookieSecure=false` (HTTP localhost); enable Secure in prod
- New signups need passwords ≥ 12 chars and not on the common denylist
- Prod profile requires `cors_allowed_origins` (no default)

## Pointers

| Item | Location |
|------|----------|
| Spec | `tasks/security-backlog.md` |
| Tasks | `tasks/todo.md` |
| Project map | `.agent/PROJECT.md` |
| Backend main | `backend/src/main/java/com/ros/ewallet/EWalletApplication.java` |
| Ownership helper | `backend/.../com/ros/ewallet/security/SecurityAccess.java` |
| Auth rate limiter | `backend/.../com/ros/ewallet/security/AuthRateLimiter.java` |
| Token denylist | `backend/.../com/ros/ewallet/security/TokenDenylist.java` |
| Amount validation test | `backend/.../com/ros/ewallet/dto/request/AmountValidationTest.java` |
| Env template | `.env.example` |
| Prod error config | `backend/src/main/resources/application-prod.yml` |
| Password denylist | `backend/.../com/ros/ewallet/validator/NotCommonPasswordValidator.java` |
