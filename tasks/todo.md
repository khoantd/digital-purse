# Tasks

> Actionable checklist for the current feature or sprint. Sync with `.agent/SESSION.md` on `/handoff`.

## Current sprint — User activity log

- [x] Flyway V16 `activity_log` + ActivityLogService / GET `/activity-logs`
- [x] Instrument auth, org, customer, wallet CRUD, spend, reverse
- [x] ActivityLogServiceTest + existing service tests green
- [x] Frontend `/activity` (OWNER/ADMIN) + nav gate
- [x] Docs (features) / SESSION

## Done (prior — Approvals queue stats)

- [x] `/approvals` OpsStatCards (Pending / Approved / Rejected / Awaiting)
- [x] Click-to-filter Status + actingId on Approve/Reject
- [x] SESSION handoff

## Done (prior — Customers & Approvals filters)

- [x] `GET /customers?status=ACTIVE|ARCHIVED|ALL` (default ACTIVE) + CustomerServiceTest
- [x] Customers toolbar: Search, Status, Linked, Clear + empty-match
- [x] Approvals toolbar: Status, Operation, From/To, Clear + empty-match
- [x] Docs (SPEC, features) / SESSION

## Done (prior — Transaction reverse)

- [x] Flyway V15: type Reverse, `reverses_transaction_id`, spend `source_transaction_id`
- [x] PaymentRail refunds + LedgerService.postReverse
- [x] TransactionReverseService (OWNER/ADMIN, 72h, dual-control, quota, idempotency)
- [x] SpendRequest OP_REVERSE approve path
- [x] `POST /transactions/{id}/reverse` + response reverse fields
- [x] Frontend Reverse action + Approvals + Reverse type filter
- [x] Docs / SESSION

## Done (prior — Wallet click → transactions; hide Receive)

- [x] SME wallet cards navigate to `/transactions?walletId=…` (Wallets + Dashboard)
- [x] Transactions page wallet filter from URL + dismissible chip; Clear removes it
- [x] Hide Receive entry points (Wallets button, Dashboard quick action); keep `/wallets/receive` route + page

## Done (prior — Transactions filter-aware stats)

- [x] OpsStatCards on `/transactions` (Transferred / Withdrawn / Received / Pending)
- [x] Stats derive from page filters (type / status / date) + spend-requests for pending

## Done (prior — Transaction subscription quota)

- [x] Flyway V14 `transaction_quota` on organization (default/backfill 1000)
- [x] `TransactionQuotaService` hard-blocks transfer/withdraw/top-up when used ≥ quota
- [x] `GET/PUT /organizations/{id}/subscription` (PUT = ROLE_ADMIN); assign default on org create
- [x] Settings Subscription tab (used / quota / remaining)
- [x] Tests: TransactionQuotaServiceTest + OrganizationServiceTest + WalletServiceTest

## Done (prior — Editable org limits)

- [x] Flyway V13 org limit columns + defaults from `app.limits`
- [x] GET/PUT `/organizations/{id}/limits` (OWNER/ADMIN); enforce per-org in TransactionLimitService
- [x] Settings Controls form editable for OWNER/ADMIN
- [x] Tests: OrganizationServiceTest + TransactionLimitServiceTest

## Done (prior — Transaction filters)

- [x] Flyway V12 type catalog (Transfer / Withdraw / Top-up) + ledger backfill
- [x] Server + forms set correct typeId on money ops
- [x] `/transactions` filters: type, status, date range + Clear

## Done (prior — Organization Settings)

- [x] Backend GET/PUT org, member role update/remove, GET limits + last-OWNER guard
- [x] OrganizationServiceTest (9)
- [x] Frontend `/settings` (General / Members / Controls) + nav + AccountPopover
- [x] Docs (features, SPEC API surface, SESSION)

## Done (prior — Customer Data)

- [x] Spec + docs (`docs/SPEC-customer-data.md`, features, SESSION)
- [x] Flyway V9 + Customer entity / repository
- [x] CustomerService + Controller + role gate (OWNER/ADMIN/ACCOUNTANT)
- [x] CustomerServiceTest (isolation, roles, link/unlink, unique conflict)
- [x] Frontend Customers page + nav + transfer picker

## Done (prior — SME multi-tenant)

- [x] Spec + docs (`docs/SPEC-sme-multi-tenant.md`, `docs/features.md`)
- [x] Flyway V8 + Organization / Membership; wallet `organization_id`
- [x] SecurityAccess org checks; limits / idempotency by org; signup default org
- [x] Org APIs + frontend org switcher (`X-Organization-Id`)
- [x] Dual-control spend (SpendRequest + approvals UI)
- [x] Frontend aligned to org-scoped wallets + dual-control UX

## Done (prior)

- [x] VND / VN account identity + currency on wallet
- [x] PaymentRail + MockPaymentRail
- [x] Idempotency-Key on money mutations
- [x] Double-entry ledger posts
- [x] VietQR payload + `/wallets/receive` UI
- [x] Frontend aligned to backend (currency, account labels, tx wallet cells, transfer `?tab=`, receive for all roles)
- [x] Transactional limits (per-tx + daily outbound/top-up)
- [x] SEC-04 history purge

## Backlog

- [ ] Email invite tokens / pending invitations
- [ ] Editable per-org dual-control threshold
- [ ] Self-serve / paid subscription upgrade UI
- [ ] 2FA / step-up for withdraw
- [ ] Money request / pay-by-username
- [ ] VietQR / NAPAS sandbox adapter (Receive UI hidden; route kept)
- [ ] Prod cookieSecure + CORS origins
- [ ] Activity log CSV export / retention job
