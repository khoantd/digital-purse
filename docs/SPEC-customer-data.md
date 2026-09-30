# Feature: SME Customer Data (payee directory)

## Objective

Let each Organization manage a **customer / payee contact book** and optionally **link** a contact to an existing in-system wallet (account number) so transfers can be prefilled.

## Target Users

| Persona | Needs |
|---------|--------|
| Owner / Admin / Accountant | Create, update, archive customers; link/unlink wallet |
| Approver | View customers (read-only) |
| Platform admin | Cross-tenant ops (same bypass as other org resources) |

## Core Features

1. **Org-scoped customers** — Contacts belong to the active Organization (`X-Organization-Id`).
2. **Contact fields** — name (required), phone, email, tax ID, notes.
3. **Optional wallet link** — Resolve by account number (IBAN column); store `linked_wallet_id`. Payee wallet may belong to any org.
4. **Soft delete** — Archive (`ARCHIVED`); default list shows `ACTIVE` only.
5. **Transfer assist** — Frontend picker fills recipient IBAN from linked customers.

### Role matrix

| Action | OWNER | ADMIN | ACCOUNTANT | APPROVER |
|--------|-------|-------|------------|----------|
| View customers | yes | yes | yes | yes |
| Create / update / archive | yes | yes | yes | no |
| Link / unlink wallet | yes | yes | yes | no |

### Acceptance criteria

- User in org X cannot read/mutate customers of org Y.
- APPROVER can list/get but receives 403 on mutations.
- Link with valid IBAN stores wallet; duplicate link in same org → 409.
- Archive removes customer from default list.
- Transfer UI can select a linked customer to prefill recipient account.

## Out of Scope

- B2B2C customer-owned sub-accounts (separate tenancy / balances outside the org)
- Creating wallets for customers / B2B2C sub-accounts as true ownership (use `ownerType=CUSTOMER` label only)
- External bank accounts as payment rails
- CSV import, KYC, invite-by-email

### Related: wallet owner type label

Wallets may be labeled `ORGANIZATION` or `CUSTOMER` (with `customer_id`) at create time. Access and limits remain organization-scoped; this does not create customer-owned wallets.

## API surface

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/customers?q=&status=` | Default `ACTIVE`; optional name search; `status` = `ACTIVE` \| `ARCHIVED` \| `ALL` |
| GET | `/api/v1/customers/{id}` | Same org |
| POST | `/api/v1/customers` | Create |
| PUT | `/api/v1/customers/{id}` | Update contact fields |
| POST | `/api/v1/customers/{id}/archive` | Soft delete |
| POST | `/api/v1/customers/{id}/link-wallet` | Body `{ "iban": "..." }` |
| POST | `/api/v1/customers/{id}/unlink-wallet` | Clear link |
