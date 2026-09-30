# Digital Purse Features

**Digital Purse** for **Vietnam SMEs**: multi-tenant organizations, org-scoped wallets, money movement (top-up, withdraw, transfer), dual-control spend, transaction history, and VietQR receive. Stack: Spring Boot REST API + React SPA + PostgreSQL. Product stance: **Vietnam-first SME demo** with shared-schema tenancy and pluggable payment rails (Mock by default). Spec: [`docs/SPEC-sme-multi-tenant.md`](SPEC-sme-multi-tenant.md).

## Authentication & accounts

| Feature | Description |
|---------|-------------|
| Sign up | Register with username, email, password; new users always get `ROLE_USER` (client cannot assign roles) |
| Log in | Credential-based login; returns a short-lived access JWT |
| Token refresh | Refresh via HttpOnly cookie (`POST /api/v1/auth/refresh`) |
| Log out | Revokes refresh token via in-memory denylist (`POST /api/v1/auth/logout`) |
| Password rules | Signup: minimum 12 characters + common-password denylist |
| Roles | `ROLE_USER` and `ROLE_ADMIN` (RBAC on API and UI routes) |

## Organizations (multi-tenant)

| Feature | Description |
|---------|-------------|
| Default org on signup | Each new user gets an Organization + `OWNER` membership |
| Active org | Client sends `X-Organization-Id`; server validates membership |
| Org roles | `OWNER`, `ADMIN`, `ACCOUNTANT`, `APPROVER` (separate from platform `ROLE_*`) |
| Members | Owner/admin can add, change role, or remove members by username |
| Settings | `/settings` — org profile, team members, editable spending controls (OWNER/ADMIN), subscription usage |
| Isolation | Wallets, limits, idempotency, and spend requests are org-scoped |
| Dashboard stats | `GET /organizations/{id}/stats` — ledger totals (transfer / withdraw / receive), pending approvals, today outbound/top-up |
| Customers | Org-scoped payee/contact directory; optional link to an in-system wallet IBAN (SPEC: [`SPEC-customer-data.md`](SPEC-customer-data.md)) |
| Wallet owner type | Wallets labeled `ORGANIZATION` or `CUSTOMER` (+ customer) at create; tenancy stays org-scoped |

## Wallets

| Feature | Description |
|---------|-------------|
| Create wallet | Belongs to active Organization; `created_by` is the authenticated user; server auto-generates VN account id; explicit `ownerType` (`ORGANIZATION` \| `CUSTOMER`) with optional `customerId` |
| Owner type | Label only — tenancy stays org-scoped; CUSTOMER requires an ACTIVE customer in the active org |
| Currency | Always `VND` on create (persisted on wallet) |
| VietQR receive | Wallet responses include a static VietQR EMVCo payload; UI at `/wallets/receive` |
| List wallets | Scoped to active org (platform admin can list all) |
| Get by ID / IBAN | Lookup a single wallet (org membership required) |
| Update | Rename wallet (account id, currency, and org are not client-updatable) |
| Delete | Remove a wallet (org membership / platform admin) |
| Balance | Stored as `BigDecimal`; mutations go through transfer / add / withdraw flows |

## Money movement

| Feature | Description |
|---------|-------------|
| Add funds (top-up) | Credit via `PaymentRail` (`MockPaymentRail` instant SUCCESS) then ledger post |
| Withdraw funds | Debit via `PaymentRail` then ledger post |
| Wallet-to-wallet transfer | Move funds between two wallets in the system |
| Idempotency | Optional `Idempotency-Key` header on transfer / addFunds / withdrawFunds |
| Amount validation | Positive amounts with digit limits (`@Positive`, `@Digits`) |
| Transactional limits | Per-org: per-tx, daily outbound / top-up, dual-control threshold (defaults from `app.limits`); day = Asia/Ho_Chi_Minh; HTTP 422 on breach |
| Transaction subscription | Lifetime per-org transaction quota (default **1000** on create from `app.subscription.default-transaction-quota`); hard-blocks transfer/withdraw/top-up at quota; `GET /organizations/{id}/subscription`; platform admin `PUT` to raise |
| Dual-control spend | Transfer/withdraw/reverse at or above threshold creates pending `SpendRequest`; another OWNER/ADMIN/APPROVER must approve |
| Concurrency | Optimistic locking (`@Version`) plus pessimistic lock on debit (`findByIbanForUpdate`) |

## Ledger

| Feature | Description |
|---------|-------------|
| Double-entry | Each money movement writes balanced `ledger_entry` rows (DEBIT/CREDIT) |
| Accounts | `WALLET` (per wallet) and `SYSTEM_FLOAT` (demo cash-in/out counterpart) |
| Audit | Append-only entries linked to `transaction_id` |

## Transactions

| Feature | Description |
|---------|-------------|
| History | List transactions for a user or (admin) all transactions |
| Detail | Fetch by transaction ID or unique reference UUID |
| Status | `PENDING`, `SUCCESS`, `ERROR` |
| Types (seeded) | Transfer, Withdraw, Top-up, Reverse |
| Reverse | Compensating reverse of SUCCESS Transfer / Top-up / Withdraw within 72h; OWNER/ADMIN; dual-control above org threshold; Mock rail refunds for top-up/withdraw; at most one reverse per original |
| Audit trail | Append-only transaction records with amount, description, from/to wallets, timestamps |

## Frontend (React)

| Area | Routes / capability |
|------|---------------------|
| Auth | `/login`, `/signup`; private routes for the rest |
| Dashboard | Org-scoped balance, money totals (transferred / withdrawn / received), pending approvals, quick actions, wallet cards |
| Wallets | List, create (`/wallets/new`), receive VietQR (`/wallets/receive`) |
| Transfers | Tabs: send / add / withdraw (`/transfers`); `Idempotency-Key`; pending dual-control → Approvals |
| Transactions | Paginated history (`/transactions`); Reverse action for OWNER/ADMIN on eligible SUCCESS rows |
| Organizations | Header org switcher sends `X-Organization-Id` |
| Customers | Payee contacts CRUD + wallet link (`/customers`); filters: search, status, linked wallet |
| Approvals | Dual-control spend queue (`/approvals`); filters: status, operation, date range |
| Settings | Organization profile, members/roles, editable limits (`/settings` Controls), subscription usage (`/settings` Subscription) |
| Access control | `PrivateRoute` + `ProtectedRoute` for `ROLE_USER` / `ROLE_ADMIN` |
| UX | Material UI, Notistack toasts, Axios with auth + org headers |

## Security (implemented)

- JWT access token (short-lived, in-memory on the client) + HttpOnly refresh cookie
- Ownership / IDOR checks via `SecurityAccess` (self or admin; wallet owner; transaction participant)
- Auth rate limit: 5 requests/minute/IP (Bucket4j) → HTTP 429
- Generic auth error messages (no username/email enumeration)
- Mass-assignment hardening (server owns roles, wallet owner, account id)
- Security headers: nosniff, frame DENY, HSTS, CSP
- CORS from configured `cors_allowed_origins` (includes `Idempotency-Key`)
- Prod profile: no stack traces / `?trace=true` leak; correlation IDs
- INFO logs use entity IDs only (no IBAN, balance, or username)

## API surface (v1)

| Resource | Endpoints |
|----------|-----------|
| Auth | `POST /api/v1/auth/login`, `/signup`, `/refresh`, `/logout` |
| Organizations | `GET/POST /api/v1/organizations`, `GET/PUT /{id}`, members CRUD, `GET/PUT /{id}/limits`, `GET/PUT /{id}/subscription`, org wallets |
| Customers | `GET/POST /api/v1/customers` (`?q=&status=ACTIVE\|ARCHIVED\|ALL`), `GET/PUT /{id}`, `POST /{id}/archive`, `POST /{id}/link-wallet`, `POST /{id}/unlink-wallet` |
| Wallets | `GET/POST /api/v1/wallets`, `GET/PUT/DELETE /{id}`, `GET /iban/{iban}`, `GET /users/{userId}`, `POST /transfer`, `/addFunds`, `/withdrawFunds` |
| Spend requests | `GET /api/v1/spend-requests`, `POST /{id}/approve`, `POST /{id}/reject` |
| Transactions | `GET /api/v1/transactions`, `GET /{id}`, `GET /references/{referenceNumber}`, `GET /users/{userId}`, `POST /{id}/reverse` |

OpenAPI / Swagger UI is available via springdoc when the backend is running.

## Out of scope (not implemented)

These appear in older notes or aspirational docs but are **not** product features today:

- Password recovery / reset
- Multi-currency wallets / FX
- Live VietQR / NAPAS sandbox or licensed bank rails (Mock rail only)
- In-app FAQ, support chat, or feedback system
- Live breach-password API (local denylist only)
- 2FA, KYC / KYB tiers
- Scheduled transfers, money requests, split bills
- DB-per-tenant isolation; payroll bulk CSV; invoicing product
