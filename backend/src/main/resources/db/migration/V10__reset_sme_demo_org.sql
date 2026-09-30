-- Reset demo data: one SME organization with OWNER / ACCOUNTANT / APPROVER members.
-- Keeps platform role + transaction type reference data.

-- Clear dependent money / tenancy data (order respects FKs)
TRUNCATE TABLE ledger_entry RESTART IDENTITY CASCADE;
TRUNCATE TABLE spend_request RESTART IDENTITY CASCADE;
TRUNCATE TABLE transaction RESTART IDENTITY CASCADE;
TRUNCATE TABLE idempotency_record RESTART IDENTITY CASCADE;
TRUNCATE TABLE customer RESTART IDENTITY CASCADE;
TRUNCATE TABLE wallet RESTART IDENTITY CASCADE;
TRUNCATE TABLE organization_membership RESTART IDENTITY CASCADE;
TRUNCATE TABLE organization RESTART IDENTITY CASCADE;
TRUNCATE TABLE public.user_role RESTART IDENTITY CASCADE;
TRUNCATE TABLE public."user" RESTART IDENTITY CASCADE;

-- Sequences used by JPA (not always owned by truncated tables)
SELECT setval('ledger_entry_seq', 1, false);
SELECT setval('spend_request_seq', 1, false);
SELECT setval('transaction_seq', 1, false);
SELECT setval('idempotency_record_seq', 1, false);
SELECT setval('customer_seq', 1, false);
SELECT setval('wallet_seq', 1, false);
SELECT setval('organization_membership_seq', 1, false);
SELECT setval('organization_seq', 1, false);
SELECT setval('user_seq', 1, false);

-- Demo password for all three users: DemoPassword1!
-- bcrypt (cost 10)
INSERT INTO public."user" (id, first_name, last_name, username, email, "password")
VALUES (1, 'Mai', 'Nguyen', 'smeowner', 'owner@saoviet.demo',
        '$2b$10$CSnyLIV.pI/gjc6EtWjfBuqmMzNTVWDgiFAFE00QIKFhUfAdYUA7m');
INSERT INTO public."user" (id, first_name, last_name, username, email, "password")
VALUES (2, 'Lan', 'Tran', 'smeaccountant', 'accountant@saoviet.demo',
        '$2b$10$CSnyLIV.pI/gjc6EtWjfBuqmMzNTVWDgiFAFE00QIKFhUfAdYUA7m');
INSERT INTO public."user" (id, first_name, last_name, username, email, "password")
VALUES (3, 'Hung', 'Pham', 'smeapprover', 'approver@saoviet.demo',
        '$2b$10$CSnyLIV.pI/gjc6EtWjfBuqmMzNTVWDgiFAFE00QIKFhUfAdYUA7m');

SELECT setval('user_seq', (SELECT MAX(id) FROM public."user"));

-- Platform roles: all ROLE_USER (org roles carry SME permissions)
INSERT INTO public.user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO public.user_role (user_id, role_id) VALUES (2, 1);
INSERT INTO public.user_role (user_id, role_id) VALUES (3, 1);

INSERT INTO organization (id, name, tax_id, status, created_at)
VALUES (1, 'Sao Viet Trading', '0312345678', 'ACTIVE', NOW());

SELECT setval('organization_seq', (SELECT MAX(id) FROM organization));

INSERT INTO organization_membership (id, organization_id, user_id, role, created_at)
VALUES (1, 1, 1, 'OWNER', NOW()),
       (2, 1, 2, 'ACCOUNTANT', NOW()),
       (3, 1, 3, 'APPROVER', NOW());

SELECT setval('organization_membership_seq', (SELECT MAX(id) FROM organization_membership));

-- Two org wallets (VN account numbers; balance cache)
INSERT INTO wallet (id, version, iban, name, balance, currency, user_id, organization_id)
VALUES (1, 0, 'VN44970436100000000000000001', 'Operating', 50000000, 'VND', 1, 1),
       (2, 0, 'VN17970436100000000000000002', 'Payroll', 20000000, 'VND', 1, 1);

SELECT setval('wallet_seq', (SELECT MAX(id) FROM wallet));

-- Sample payee linked to Payroll wallet (for transfer picker demos)
INSERT INTO customer (id, organization_id, name, phone, email, tax_id, notes,
                      linked_wallet_id, created_by_user_id, created_at, updated_at, status)
VALUES (1, 1, 'Nguyen Van A', '0901234567', 'vana@example.com', NULL,
        'Sample vendor', 2, 1, NOW(), NOW(), 'ACTIVE');

SELECT setval('customer_seq', (SELECT MAX(id) FROM customer));
