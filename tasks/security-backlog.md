# Security Backlog

> Source: manual security review on 2026-09-30 (Spring Boot 4.1.1 codebase).
> Ordered by severity, then by dependency (ownership checks underpin most money paths).
> Check off items as they are fixed **and** covered by a test.

## Legend

- 🔴 Critical — exploitable now, fix before any deploy
- 🟡 High — serious abuse/fraud path, fix next
- 🟢 Medium — hardening, fix after critical/high

---

## 🔴 Critical

- [x] **SEC-01 — Privilege escalation via self-assigned roles on signup**
  - Severity: Critical
  - Files: `backend/src/main/java/com/ros/ewallet/dto/request/SignupRequest.java:43`,
    `backend/src/main/java/com/ros/ewallet/dto/mapper/SignupRequestMapper.java:48-52`,
    `backend/src/main/java/com/ros/ewallet/domain/enums/RoleType.java:11`,
    `backend/src/main/java/com/ros/ewallet/service/AuthService.java:75-85`
  - Problem: client sends `roles`; any value including `ROLE_ADMIN` is honored.
  - Fix: ignore client roles, hard-code `ROLE_USER` on signup; admin role assignment only via a separate admin-secured endpoint.
  - Accept: signup with `["ROLE_ADMIN"]` yields a `ROLE_USER`-only account (integration test).
  - Done: removed `roles` from `SignupRequest` (`@JsonIgnoreProperties(ignoreUnknown = true)`); mapper always assigns `ROLE_USER`; frontend no longer sends roles. Covered by `SignupRequestMapperTest`. Admin role API deferred (seed/DB until secured endpoint exists).

- [x] **SEC-02 — Missing ownership checks (IDOR) on wallet/transaction reads and writes**
  - Severity: Critical
  - Files: `backend/src/main/java/com/ros/ewallet/service/WalletService.java:51-106,211-243`,
    `backend/src/main/java/com/ros/ewallet/service/TransactionService.java:44-92`
  - Problem: any `ROLE_USER` can read/update/delete any wallet or transaction by id/IBAN/userId.
  - Fix: resolve caller from `SecurityContextHolder`; allow only owner (or admin). Scope list endpoints to the caller.
  - Accept: user A gets 403/404 for user B's wallet/transaction ids (tests for each endpoint).
  - Done: added `SecurityAccess` + `ForbiddenException` (403); ownership on wallet/transaction reads, lists, create/update/delete; list endpoints scoped to caller (admin sees all). Covered by `SecurityAccessTest`, `WalletServiceTest`, `TransactionServiceTest`.

- [x] **SEC-03 — Any user can transfer/withdraw from any wallet**
  - Severity: Critical
  - Files: `backend/src/main/java/com/ros/ewallet/service/WalletService.java:140-159,168-203`
  - Problem: `transferFunds`/`withdrawFunds`/`addFunds` never verify the caller owns `fromWallet`; funds-theft path by IBAN.
  - Fix: enforce debit-wallet ownership (owner or admin) before balance mutation. Depends on SEC-02 principal plumbing.
  - Accept: transferring from another user's wallet is rejected; own-wallet transfer still works (tests).
  - Done: `transferFunds`/`withdrawFunds` require ownership of debit wallet; `addFunds` requires ownership of credit wallet. Covered by `WalletServiceTest` ownership rejection + happy-path tests.

- [x] **SEC-04 — Secrets committed to git**
  - Severity: Critical
  - Files: `.env.properties` (tracked); history contains prior `jwt_secret` values
  - Problem: DB password + JWT secret in repo; old weak secret lives in history.
  - Fix: gitignore `.env.properties`, add `.env.example` placeholders, rotate DB password + JWT secret in all envs, purge history (`git filter-repo`) if repo is/was public.
  - Accept: `git log --all -- .env.properties` shows no secrets; fresh clone runs from `.env.example`.
  - Done: `.env.properties` / `.env` gitignored + removed from index; `.env.example` placeholders only; local `jwt_secret` + `db_password` rotated (2026-09-30); **git history purged** with `git filter-repo` (removed `^\.env$` and `^\.env\.properties$` from all commits); **force-pushed** rewritten `master` to `origin` (`88f90e5...8f0f313`).

- [x] **SEC-05 — No amount validation (ledger manipulation)**
  - Severity: Critical
  - Files: `backend/src/main/java/com/ros/ewallet/dto/request/TransactionRequest.java:25-26`,
    `backend/src/main/java/com/ros/ewallet/dto/request/WalletRequest.java:31-32`
  - Problem: `amount`/`balance` accept negative/zero → negative top-up withdraws, negative transfer reverses direction.
  - Fix: `@Positive` (or `@DecimalMin("0.01")`) + `@Digits` on both fields.
  - Accept: negative/zero amounts rejected with 422 (validation tests).
  - Done: `@Positive` + `@Digits(integer=12, fraction=2)` on `amount`/`balance`; messages added. Covered by `AmountValidationTest` (Bean Validation → 422 via existing `GlobalExceptionHandler`).

## 🟡 High

- [x] **SEC-06 — Mass assignment of server-controlled fields**
  - Severity: High
  - Files: `backend/src/main/java/com/ros/ewallet/dto/request/TransactionRequest.java`,
    `backend/src/main/java/com/ros/ewallet/dto/mapper/TransactionRequestMapper.java`,
    `backend/src/main/java/com/ros/ewallet/dto/request/WalletRequest.java`,
    `backend/src/main/java/com/ros/ewallet/service/WalletService.java`
  - Problem: client controls `id` (flows into `save()` → merge risk), `userId` (create wallet for another user), opening `balance`.
  - Fix: remove `id`/`status`/`referenceNumber`/`createdAt` from request DTOs (or `@Null` + mapper ignores); derive `userId` from auth principal.
  - Accept: server-generated fields ignore/forbid client values (mapper + API tests).
  - Done: stripped server fields from `TransactionRequest`; mapper always sets status/ref/createdAt; wallet `id` ignored; `userId` overwritten from principal; update mutates name only (IBAN assigned at create). `TransactionRequestMapperTest` + `WalletServiceTest.create_shouldIgnoreClientUserId…`.

- [x] **SEC-07 — JWT in `localStorage`, no revocation**
  - Severity: High
  - Files: `frontend/src/services/AuthService.js`, `frontend/src/services/AuthHeader.js`,
    `backend/src/main/java/com/ros/ewallet/security/JwtUtils.java`
  - Problem: any XSS = full account takeover; 1h stateless token, no logout/blacklist.
  - Fix: short-lived access token in memory + rotating HttpOnly `Secure; SameSite=Strict` refresh cookie; server logout denylist.
  - Accept: token absent from `localStorage`; logout invalidates session (frontend + backend tests).
  - Done: 15m access JWT (memory only) + 7d refresh HttpOnly cookie (`SameSite=Strict`; `cookieSecure` false for local HTTP); `/auth/refresh` + `/auth/logout` with `TokenDenylist`; frontend `withCredentials` + silent refresh on boot. Tests: `TokenDenylistTest`, `AuthServiceTest.logout_…`.

- [x] **SEC-08 — No brute-force protection on auth endpoints**
  - Severity: High
  - Files: `backend/src/main/java/com/ros/ewallet/controller/AuthController.java`,
    `backend/src/main/java/com/ros/ewallet/security/AuthRateLimiter.java`
  - Problem: unlimited `/login` + `/signup` attempts.
  - Fix: rate-limit (e.g. Bucket4j: 5 logins/min/IP + account lockout with backoff); log/alert spikes.
  - Accept: 6th rapid login attempt throttled (test with mocked limiter or integration test).
  - Done: Bucket4j 5/min/IP on login/signup/refresh → 429.`AuthRateLimiterTest`.

- [x] **SEC-09 — User enumeration via signup/login messages**
  - Severity: High
  - Files: `backend/src/main/java/com/ros/ewallet/service/AuthService.java`
  - Problem: distinct `USERNAME_EXISTS` vs `EMAIL_EXISTS` (and likely login) messages.
  - Fix: generic "credentials already in use / invalid credentials" responses; uniform timing.
  - Accept: enumeration probes return indistinguishable responses (tests).
  - Done: signup always checks username+email and returns `ERROR_CREDENTIALS_IN_USE`; login/user-not-found uses generic Unauthorized. `AuthServiceTest` covers both conflict paths.

- [x] **SEC-10 — Race condition allows double-spend**
  - Severity: High
  - Files: `backend/src/main/java/com/ros/ewallet/domain/entity/Wallet.java`,
    `backend/src/main/java/com/ros/ewallet/service/WalletService.java`,
    `backend/src/main/resources/db/migration/V6__wallet_version.sql`
  - Problem: no `@Version`; concurrent transfers can both pass the balance check.
  - Fix: `@Version` optimistic locking on `Wallet` with retry, or pessimistic lock on debit wallet; add concurrent-transfer test.
  - Accept: parallel overdraft attempts leave balance consistent, one fails (concurrency test).
  - Done: `@Version` + Flyway `version` column; `findByIbanForUpdate` (PESSIMISTIC_WRITE) on debit paths. `WalletConcurrencyTest`.

## 🟢 Medium

- [x] **SEC-11 — Stack-trace / message disclosure via `?trace=true`**
  - Severity: Medium
  - Files: `backend/src/main/resources/application.yml`,
    `backend/src/main/resources/application-prod.yml`,
    `backend/src/main/java/com/ros/ewallet/exception/GlobalExceptionHandler.java`
  - Problem: `include-message: always` + `include-stacktrace: on_param` + `exception.trace: true` expose internals on demand.
  - Fix: prod profile → `include-message: never` (or `on_param`), `include-stacktrace: never`, `exception.trace: false`; log server-side with correlation id.
  - Accept: prod error responses carry no stack/message internals (config + handler test).
  - Done: default `exception.trace: false` + `include-stacktrace: never`; `application-prod.yml` hardens messages; 5xx clients get generic message + correlationId; stack only when trace enabled. `GlobalExceptionHandlerTest`.

- [x] **SEC-12 — Missing security headers**
  - Severity: Medium
  - Files: `backend/src/main/java/com/ros/ewallet/config/SecurityConfig.java`
  - Problem: no HSTS/CSP/`X-Content-Type-Options`/frame options.
  - Fix: enable Spring Security header defaults + HSTS for prod; add CSP for the React frontend.
  - Accept: responses carry the header set (MockMvc header assertions).
  - Done: explicit headers (nosniff, DENY frame, HSTS 1y, CSP `default-src 'self'`). `SecurityHeadersAndCorsTest`.

- [x] **SEC-13 — Weak password policy + password `.trim()`**
  - Severity: Medium
  - Files: `backend/src/main/java/com/ros/ewallet/dto/request/SignupRequest.java`,
    `backend/src/main/java/com/ros/ewallet/service/AuthService.java`,
    `backend/src/main/java/com/ros/ewallet/validator/NotCommonPassword.java`
  - Problem: min 6 chars, no breach-list check; login `.trim()`s passwords, altering credentials.
  - Fix: min 10–12 chars (+ breach-list check); hash exactly what was sent.
  - Accept: short passwords rejected; password with spaces authenticates verbatim (tests).
  - Done: signup min 12 + `@NotCommonPassword` denylist; login no longer trims password. `PasswordPolicyTest`, `AuthServiceTest.login_shouldAuthenticateWithPasswordVerbatimIncludingSpaces`.

- [x] **SEC-14 — Sensitive data in logs**
  - Severity: Medium
  - Files: `backend/src/main/java/com/ros/ewallet/service/WalletService.java`,
    `backend/src/main/java/com/ros/ewallet/service/TransactionService.java`,
    `backend/src/main/java/com/ros/ewallet/service/AuthService.java`,
    `backend/src/main/resources/messages.properties`
  - Problem: IBANs, balances, usernames at INFO into `./logs/application.log`.
  - Fix: drop/mask PII and balances in logs; use append-only audit table for money movement.
  - Accept: no IBAN/balance in log output (log-capture test or manual verify).
  - Done: INFO logs use entity IDs only; transaction rows remain the money-movement audit trail. `SensitiveLoggingTest`.

- [x] **SEC-15 — CORS origin hard-coded, wildcard headers**
  - Severity: Medium
  - Files: `backend/src/main/java/com/ros/ewallet/config/SecurityConfig.java`,
    `backend/src/main/resources/application.yml`, `.env.example`
  - Problem: `localhost:3000` baked in; `setAllowedHeaders("*")`.
  - Fix: externalize allowed origins per environment; enumerate required headers once cookies/credentials are used.
  - Accept: prod serves frontend origin from config; preflight succeeds (test).
  - Done: `app.security.cors.allowed-origins` / `cors_allowed_origins`; enumerated CORS headers. `SecurityHeadersAndCorsTest`.

---

## Suggested build order

1. SEC-01, SEC-05, SEC-06 (self-contained, test-verifiable)
2. SEC-02 + SEC-03 (core banking invariant; needs auth-principal plumbing)
3. SEC-04 (before any deploy)
4. SEC-10 (financial correctness)
5. SEC-07, SEC-08, SEC-11, SEC-12
6. SEC-09, SEC-13, SEC-14, SEC-15
