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

SME multi-tenant Digital Purse foundation delivered (Organization tenancy + dual-control spend + Customer Data). Specs: `docs/SPEC-sme-multi-tenant.md`, `docs/SPEC-customer-data.md`.

## Done

- **Vietnam foundation slice** — VN account ids; wallet `currency=VND`; Flyway `V7`
- **PaymentRail / Idempotency / Ledger / VietQR** — as prior
- **Transactional limits** — per-tx / daily outbound / daily top-up
- **SEC-04 history purge** — completed
- **SME multi-tenant (2026-09-30):**
  - Spec + docs; Flyway `V8` (organization, membership, spend_request, org-scoped wallets/idempotency)
  - `SecurityAccess` membership/role checks; `X-Organization-Id` filter
  - Signup creates default org + OWNER; limits/idempotency by organization
  - Dual-control spend above `app.limits.dual-control-threshold` (default 10M VND)
  - Frontend org switcher + Approvals page
  - Backend tests: **119** green (incl. SpendRequestServiceTest)
- **UI alignment with SME/org changes (2026-09-30):**
  - Dashboard / Add funds / Receive use org-scoped `/wallets` (not `/wallets/users/{id}`)
  - `organization-changed` refresh on Dashboard, Transactions, Receive, Add funds
  - Pending dual-control redirects to `/approvals`; Approvals table shows amount, accounts, status chips
  - Org switcher shows role; SME copy on auth + wallet/transfer pages
- **Customer Data (2026-09-30):**
  - Spec `docs/SPEC-customer-data.md`; Flyway `V9` (`customer` table + optional `linked_wallet_id`)
  - `CustomerService` / `CustomerController` — CRUD, archive, link/unlink wallet
  - Manage roles: OWNER / ADMIN / ACCOUNTANT; all members can list
  - Frontend `/customers` + nav; transfer Autocomplete prefills linked IBAN
  - `CustomerServiceTest` green (8 tests)
- **Demo data reset (2026-09-30):** Flyway `V10` wipes old users/wallets/orgs; seeds **Sao Viet Trading** with `smeowner` (OWNER), `smeaccountant` (ACCOUNTANT), `smeapprover` (APPROVER)
- **Wallet owner type label (2026-09-30):**
  - Flyway `V11` — `wallet.owner_type` + optional `customer_id` (check constraint)
  - Create requires explicit `ownerType` (`ORGANIZATION` | `CUSTOMER`); CUSTOMER needs ACTIVE customer in active org
  - Response exposes `ownerType`, `customerId`, `customerName`; New Wallet UI + list/cards show type
  - Tenancy unchanged (org-scoped); not B2B2C sub-accounts
  - `WalletServiceTest` (32) + `AmountValidationTest` (7) green
- **Organization Settings (2026-09-30):**
  - Backend: `GET/PUT /organizations/{id}`, member role update/remove, `GET /{id}/limits`; last-OWNER guard
  - `OrganizationServiceTest` (9) green
  - Frontend `/settings` — General / Members / Controls tabs; nav + AccountPopover wired
  - OWNER/ADMIN mutate; other roles read-only; Mobbin-inspired member table + invite dialog
- **Dashboard ops stats (2026-09-30):**
  - `GET /organizations/{id}/stats` — ledger totals (transfer / withdraw / receive), pending approvals, today outbound/top-up
  - `OrganizationStatsService` + `OrganizationStatsServiceTest` (4) green
  - Dashboard SME + org-context admin: Transferred / Withdrawn / Received / Pending widgets
- **Dashboard UI alignment (2026-09-30):**
  - Ops stats + quick actions share `Grid spacing={2}` / `md={3}` so column edges align
  - `QuickActionButton` fills grid cell (`width: 100%`, `minHeight: 88`)
  - Receive icon fixed to `ant-design:qrcode-outlined` (Eva QR glyph was blank)
  - `WalletCard` / `OpsStatCard` equal-height polish within rows
- **Wallets page sections (2026-09-30):** `/wallets` splits Organization vs Customers' by `ownerType` (cards + admin tables)
- **Dashboard wallet sections (2026-09-30):** home preview mirrors `/wallets` — Organization Wallets + Customers' Wallets by `ownerType`
- **Transaction filters (2026-09-30):** `/transactions` type / status / date range; Flyway `V12` type catalog + create-path `TYPE_*`
- **Transaction subscription quota (2026-09-30):** lifetime org `transaction_quota` (default 1000); hard-block money ops; Settings Subscription tab
- **Wallet click → transactions; Receive hidden (2026-09-30):** cards → `/transactions?walletId=`; Receive UI entry points removed; receive route kept
- **Wallet card Details / Edit (2026-09-30):** `/wallets` cards — Details + Edit dialogs (rename via PUT); admin menu wired; Dashboard preview unchanged
- **Transaction reverse (2026-09-30):** Compensating reverse for SUCCESS Transfer / Top-up / Withdraw; Flyway `V15`; OWNER/ADMIN + 72h window + dual-control; Mock rail refunds; UI Reverse on `/transactions`
- **List pagination (2026-09-30):** Client-side `TablePagination` on `/wallets` (per section), `/customers`, `/approvals`; `/transactions` footer aligned
- **Customers & Approvals filters (2026-09-30):** `/customers` Search + Status + Linked + Clear; `GET /customers?status=`; `/approvals` Status + Operation + date range + Clear; empty-match states; CustomerServiceTest (11)
- **Customers directory stats (2026-09-30):** `/customers` OpsStatCards (Active / Archived / Linked / Unlinked); client-side status filter after `status=ALL` load

## In progress

- _(idle)_

## Next

1. Optional: email invite tokens / pending invitations
2. Optional: self-serve / paid subscription upgrade UI
3. Optional: 2FA, money request, VietQR sandbox (Receive UI hidden; `/wallets/receive` kept), prod cookie/CORS
4. After pull: run Flyway `V8`–`V15` (owner type + transaction types + org limits + transaction quota + reverse)
5. Optional: relax `WalletRequest` so update does not require `@Positive` balance (rename currently sends placeholder when balance is 0)

## Done (recent)

- **Customers directory stats (2026-09-30):** `/customers` OpsStatCards — Active / Archived / Linked / Unlinked; load `status=ALL` + client status/linked filters; click toggles; sr-only active count live status.
- **Approvals queue stats (2026-09-30):** `/approvals` OpsStatCards — Pending / Approved / Rejected (counts) + Awaiting (pending VND); scoped by operation+date; click toggles Status filter; actingId on Approve/Reject; sr-only pending live status.
- **Customers & Approvals filters (2026-09-30):** Card toolbars aligned with Transactions. Customers: labeled Search (Enter/Search), Status (ACTIVE/ARCHIVED/ALL, default ACTIVE), Linked (client-side), Clear; backend `?status=`; empty vs no-match. Approvals: Status, Operation, From/To, Clear; client-side; empty vs no-match. Spec/features updated; CustomerServiceTest 11 green.
- **List pagination (2026-09-30):** Client-side MUI `TablePagination` on `/wallets` (Organization + Customers' sections), `/customers`, `/approvals`; `/transactions` footer label + divider aligned. Defaults 5/10/25; page resets on reload/org/search.
- **Transaction reverse (2026-09-30):** Flyway `V15` type Reverse + `reverses_transaction_id` + spend `source_transaction_id`; `TransactionReverseService`; `POST /transactions/{id}/reverse`; Mock `refundTopUp`/`refundWithdraw`; dual-control `OP_REVERSE`; Transactions Reverse action + Approvals label; tests green (Reverse 6 + Ledger 4 + MockRail 4 + related).
- **Wallet card Details / Edit (2026-09-30):** `/wallets` SME cards show labeled Details + Edit footer actions (`stopPropagation`); Details dialog (full IBAN + copy, balance, type, View transactions); Edit dialog renames via `PUT /wallets/{id}` (balance/owner read-only). Admin table menu: Details + Edit enabled; Delete still disabled. Dashboard cards unchanged (no action props).
- **Wallet click → transactions; hide Receive (2026-09-30):** SME wallet cards on `/wallets` and Dashboard open `/transactions?walletId=…` (optional `walletName`); Transactions filters by from/to wallet id + dismissible chip; Clear removes wallet param. Receive button/quick action removed; `/wallets/receive` + `ReceiveFunds.js` kept for later.
- **Transactions page filter-aware stats (2026-09-30):** `/transactions` OpsStatCards (Transferred / Withdrawn / Received / Pending approvals) aggregate from filtered list + spend-requests; “Today” receive subtitle uses Asia/Ho_Chi_Minh.
- **Transaction subscription quota (2026-09-30):** Flyway `V14` `organization.transaction_quota` (default/backfill 1000); new orgs get `app.subscription.default-transaction-quota`; `TransactionQuotaService` hard-blocks transfer/withdraw/top-up at quota (422); `GET/PUT /organizations/{id}/subscription` (PUT = platform ADMIN); Settings Subscription tab; tests green (Quota 5 + Org 15 + Wallet 33).
- **Editable org limits (2026-09-30):** Flyway `V13` stores per-tx / daily outbound / daily top-up / dual-control on `organization`; `PUT /organizations/{id}/limits` (OWNER/ADMIN); `TransactionLimitService` enforces org values; Settings Controls form editable for manage roles.
- **Transaction filters (2026-09-30):** `/transactions` toolbar — type (Transfer/Withdraw/Top-up), status, date From/To, Clear; client-side on loaded list. Flyway `V12` renames types + ledger backfill; `WalletService` forces `TYPE_*` on create.
- **Dashboard wallet sections (2026-09-30):** home preview splits Organization vs Customers' by `ownerType`; View all → `/wallets`; BalanceHero unchanged.
- **Wallets page sections (2026-09-30):** `/wallets` splits into Organization Wallets + Customers' Wallets by `ownerType` (SME cards + admin tables); per-section empty states; no backend change.
- **Dashboard UI alignment (2026-09-30):** shared Grid for stats/actions; Receive icon; wallet card rhythm — see Done above.
- **Dashboard ops stats (2026-09-30):** org money totals + pending approvals on Dashboard; see Done above.
- **Organization Settings (2026-09-30):** org profile + members CRUD UI; see Done above.
- **Wallet owner type (2026-09-30):** label on create; see Done above.
- **Org header race (2026-09-30):** `GET /wallets` returned 403 when Dashboard loaded before `X-Organization-Id` was set. Fixed via `ensureActiveOrganization()` on refresh + login; OrgSwitcher reuses it. Empty `GET /transactions/users/{id}` 404 remains intentional (`getListWithAuth`).

## Decisions

- Product: **Vietnam SME demo** with shared-schema multi-tenancy — not a licensed bank claim
- Tenant = `Organization`; wallets owned by org; users access via `OrganizationMembership` + `OrganizationRole`
- Active org via header `X-Organization-Id`
- Platform `ROLE_ADMIN` remains cross-tenant ops; org roles are separate
- Account id column remains `iban`; generator emits `VN…`; UI labels say “account number”
- Ledger keeps wallet `balance` as cache; entries are the financial audit
- Limits and idempotency scoped by **organization**
- **Dual-control threshold** under `app.limits.dual-control-threshold` (platform default for new orgs)
- **Org limits** are DB-backed on `organization` (per-tx / daily outbound / daily top-up / dual-control); OWNER/ADMIN edit via Settings Controls / `PUT /organizations/{id}/limits`; `app.limits` = defaults only
- **Transaction reverse** = compensating new `TYPE_REVERSE` linked via `reverses_transaction_id` (unique); OWNER/ADMIN; 72h window (`app.limits.reverse-window-hours`); dual-control same threshold; Mock rail refunds for top-up/withdraw; does not consume daily limits; counts toward subscription quota
- **Transaction subscription** = lifetime allotment (`transaction_quota`); default 1000; never resets; hard-block money ops at quota; only platform `ROLE_ADMIN` raises quota (no billing UI yet)
- **Customers** = org-scoped payee/contact directory (not sub-wallets); optional link to any in-system wallet by IBAN
- Customer manage = OWNER/ADMIN/ACCOUNTANT; APPROVER read-only
- **Wallet owner type** = classification label (`ORGANIZATION` / `CUSTOMER` + customerId); does not change tenancy or create customer-owned wallets; link-wallet remains separate
- **Settings** = org profile + members + editable Controls + subscription usage (OWNER/ADMIN); other roles read-only
- Frontend UX: org switcher + existing MUI Minimal patterns; Settings layout from Mobbin team-settings patterns
- Dev: Docker Compose runs Postgres only; backend/frontend run locally
- SEC-04: history rewrite done; collaborators must re-clone

## Gotchas

- Open `backend/` as the IDE project root for Spring Boot (not monorepo root)
- Env vars in root `.env.properties`; copy from `.env.example`
- Flyway `V6`–`V15` must run before app start after pull
- Mockito needs agent attach (full JVM permissions for tests)
- List GETs return **404** when empty — frontend maps via `getListWithAuth`
- Non-admin wallet list requires **`X-Organization-Id`** — bootstrap via `frontend/src/services/ensureOrganization.js` before dashboard loads
- Local auth cookies: `cookieSecure=false`; enable Secure in prod
- Demo users (org **Sao Viet Trading**): `smeowner` / `smeaccountant` / `smeapprover` — password `DemoPassword1!` — org roles OWNER / ACCOUNTANT / APPROVER

## Pointers

| Item | Location |
|------|----------|
| SME spec | `docs/SPEC-sme-multi-tenant.md` |
| Customer spec | `docs/SPEC-customer-data.md` |
| Features | `docs/features.md` |
| Tasks | `tasks/todo.md` |
| Project map | `.agent/PROJECT.md` |
| IBAN / VN generator | `backend/.../service/IbanGenerator.java` |
| VietQR | `backend/.../service/VietQrGenerator.java` |
| Payment rail | `backend/.../rail/PaymentRail.java`, `MockPaymentRail.java` |
| Ledger | `backend/.../service/LedgerService.java` |
| Idempotency | `backend/.../service/IdempotencyService.java` |
| Transaction limits | `backend/.../service/TransactionLimitService.java` |
| Transaction quota | `backend/.../service/TransactionQuotaService.java` |
| Customers | `backend/.../service/CustomerService.java`, `frontend/.../pages/customers/Customers.js` |
| Settings | `OrganizationService`, `frontend/.../pages/settings/Settings.js` |
| Wallet owner type | `WalletOwnerType`, migration `V11__wallet_owner_type.sql` |
| Migration V7 | `backend/.../db/migration/V7__vietnam_ledger_idempotency.sql` |
| Migration V8 | `backend/.../db/migration/V8__organization_tenancy.sql` |
| Migration V9 | `backend/.../db/migration/V9__customer.sql` |
| Migration V10 | `backend/.../db/migration/V10__reset_sme_demo_org.sql` |
| Migration V11 | `backend/.../db/migration/V11__wallet_owner_type.sql` |
| Migration V12 | `backend/.../db/migration/V12__transaction_types.sql` |
| Migration V13 | `backend/.../db/migration/V13__organization_limits.sql` |
| Migration V14 | `backend/.../db/migration/V14__organization_transaction_quota.sql` |
| Migration V15 | `backend/.../db/migration/V15__transaction_reverse.sql` |
| Transaction reverse | `TransactionReverseService`, `POST /transactions/{id}/reverse` |
| Transaction filters | `frontend/src/pages/transaction/Transaction.js` |
| Receive UI | `frontend/src/pages/wallet/ReceiveFunds.js` |
| Transfer tabs | `frontend/src/pages/transfer/BasicTabs.js` |
