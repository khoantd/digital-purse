# Feature: SME multi-tenant Digital Purse

## Objective

Reposition the Vietnam-first **Digital Purse** for **SME** customers using shared-schema multi-tenancy: each business is an **Organization**; wallets and limits belong to the org; users act through membership and org roles. First SME control after tenancy: **dual-control spend**.

## Target Users

| Persona | Needs |
|---------|--------|
| Business owner | Create org, wallets, invite staff, approve large spends |
| Admin / accountant | Day-to-day top-up, transfer, withdraw within role |
| Approver | Approve or reject spend requests above threshold |
| Platform admin (`ROLE_ADMIN`) | Cross-tenant support (ops) |

## Core Features

1. **Organization tenancy** — Create org on signup (default personal business); migrate existing users to one org each; wallets require `organization_id`; API scoped by `X-Organization-Id`.
2. **Membership & org roles** — `OWNER`, `ADMIN`, `ACCOUNTANT`, `APPROVER`; owner can add members by username.
3. **Org-scoped money paths** — Wallet CRUD and money mutations authorize via org membership/role; daily limits and idempotency keyed by organization.
4. **Dual-control spend** — Transfer/withdraw at or above the organization's dual-control threshold create a `SpendRequest` (`PENDING`); a different member with `APPROVER`/`OWNER`/`ADMIN` approves (executes) or rejects.

### Role matrix

| Action | OWNER | ADMIN | ACCOUNTANT | APPROVER |
|--------|-------|-------|------------|----------|
| View wallets / transactions | yes | yes | yes | yes |
| Create wallet / top-up | yes | yes | yes | no |
| Transfer / withdraw (initiate) | yes | yes | yes | no |
| Approve / reject spend | yes | yes | no | yes |
| Manage members | yes | yes | no | no |
| Delete org | yes | no | no | no |

### Acceptance criteria

- User A in org X cannot read/mutate wallets of org Y (403/404).
- Signup creates user + default Organization + `OWNER` membership.
- Flyway migrates existing wallets onto a personal org per user.
- Money mutation without membership in active org is rejected.
- Transfer/withdraw ≥ dual-control threshold does not debit until approved; initiator cannot self-approve.

## Out of Scope

- DB / schema per tenant
- KYB, licensed NAPAS rails
- Payroll CSV, invoicing product
- Complex multi-org UX beyond a simple org switcher

## Technical Approach

- Shared Postgres schema; `organization_id` on wallet, idempotency, spend_request
- Request header `X-Organization-Id` (validated against membership); platform admin may access without
- Keep global `ROLE_USER` / `ROLE_ADMIN`; org roles are separate (`OrganizationRole`)
- Vietnam ledger / PaymentRail / VietQR unchanged except org scoping on authz and limits

## API surface (additions)

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/organizations` | Orgs for current user |
| POST | `/api/v1/organizations` | Create org (caller becomes OWNER) |
| GET | `/api/v1/organizations/{id}` | Org details + caller role |
| PUT | `/api/v1/organizations/{id}` | Update name / taxId (OWNER/ADMIN) |
| GET | `/api/v1/organizations/{id}/members` | List members |
| POST | `/api/v1/organizations/{id}/members` | Add member by username + role |
| PUT | `/api/v1/organizations/{id}/members/{membershipId}` | Change role (OWNER/ADMIN; cannot assign OWNER) |
| DELETE | `/api/v1/organizations/{id}/members/{membershipId}` | Remove member (blocks last OWNER) |
| GET | `/api/v1/organizations/{id}/limits` | Org transactional limits (VND) |
| PUT | `/api/v1/organizations/{id}/limits` | Update limits (OWNER/ADMIN); `app.limits` = defaults for new orgs |
| GET | `/api/v1/organizations/{id}/subscription` | Lifetime transaction quota usage (`transactionQuota`, `transactionUsed`, `remaining`) |
| PUT | `/api/v1/organizations/{id}/subscription` | Raise quota (platform `ROLE_ADMIN` only); cannot set below used count |
| GET | `/api/v1/organizations/{id}/wallets` | Org wallets |
| GET | `/api/v1/spend-requests` | Pending (and recent) for active org |
| POST | `/api/v1/spend-requests/{id}/approve` | Execute money movement |
| POST | `/api/v1/spend-requests/{id}/reject` | Cancel |

Existing wallet money endpoints remain; above-threshold outbound returns status `PENDING_APPROVAL`.

## Success Metrics

- Backend unit/integration tests green including org isolation and dual-control
- Frontend can switch org and send `X-Organization-Id`
- Docs (`features.md`, SESSION) reflect SME + multi-tenant stance
