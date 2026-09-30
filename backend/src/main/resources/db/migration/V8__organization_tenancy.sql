-- SME multi-tenant: organization, membership, org-scoped wallets, spend requests, org-scoped idempotency

CREATE SEQUENCE IF NOT EXISTS organization_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS organization_membership_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS spend_request_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE organization
(
    id         BIGINT                      NOT NULL,
    name       VARCHAR(100)                NOT NULL,
    tax_id     VARCHAR(20),
    status     VARCHAR(20)                 NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_organization PRIMARY KEY (id)
);

CREATE TABLE organization_membership
(
    id              BIGINT                      NOT NULL,
    organization_id BIGINT                      NOT NULL,
    user_id         BIGINT                      NOT NULL,
    role            VARCHAR(20)                 NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_organization_membership PRIMARY KEY (id)
);

ALTER TABLE organization_membership
    ADD CONSTRAINT fk_org_membership_on_organization FOREIGN KEY (organization_id) REFERENCES organization (id);

ALTER TABLE organization_membership
    ADD CONSTRAINT fk_org_membership_on_user FOREIGN KEY (user_id) REFERENCES public."user" (id);

CREATE UNIQUE INDEX uniq_org_membership_org_user
    ON organization_membership (organization_id, user_id);

CREATE INDEX idx_org_membership_user_id ON organization_membership (user_id);

-- Backfill: one personal organization per existing user
INSERT INTO organization (id, name, tax_id, status, created_at)
SELECT nextval('organization_seq'),
       LEFT(u.username || '''s business', 100),
       NULL,
       'ACTIVE',
       NOW()
FROM public."user" u;

INSERT INTO organization_membership (id, organization_id, user_id, role, created_at)
SELECT nextval('organization_membership_seq'),
       o.id,
       u.id,
       'OWNER',
       NOW()
FROM public."user" u
         JOIN organization o ON o.name = LEFT(u.username || '''s business', 100);

-- Wallet: add organization_id (nullable first), backfill, then NOT NULL
ALTER TABLE wallet
    ADD COLUMN organization_id BIGINT;

UPDATE wallet w
SET organization_id = m.organization_id
FROM organization_membership m
WHERE m.user_id = w.user_id
  AND m.role = 'OWNER';

ALTER TABLE wallet
    ALTER COLUMN organization_id SET NOT NULL;

ALTER TABLE wallet
    ADD CONSTRAINT fk_wallet_on_organization FOREIGN KEY (organization_id) REFERENCES organization (id);

DROP INDEX IF EXISTS wallet_user_id_iban_key;
DROP INDEX IF EXISTS wallet_user_id_name_key;

CREATE UNIQUE INDEX wallet_organization_id_name_key ON wallet (organization_id, name);
CREATE INDEX idx_wallet_organization_id ON wallet (organization_id);

-- Idempotency: scope by organization
ALTER TABLE idempotency_record
    ADD COLUMN organization_id BIGINT;

UPDATE idempotency_record r
SET organization_id = m.organization_id
FROM organization_membership m
WHERE m.user_id = r.user_id
  AND m.role = 'OWNER';

-- Orphaned rows (should not exist): drop if any null after backfill
DELETE FROM idempotency_record WHERE organization_id IS NULL;

ALTER TABLE idempotency_record
    ALTER COLUMN organization_id SET NOT NULL;

ALTER TABLE idempotency_record
    ADD CONSTRAINT fk_idempotency_on_organization FOREIGN KEY (organization_id) REFERENCES organization (id);

DROP INDEX IF EXISTS uniq_idempotency_user_op_key;

CREATE UNIQUE INDEX uniq_idempotency_org_op_key
    ON idempotency_record (organization_id, operation, idempotency_key);

CREATE TABLE spend_request
(
    id               BIGINT                      NOT NULL,
    organization_id  BIGINT                      NOT NULL,
    requester_id     BIGINT                      NOT NULL,
    approver_id      BIGINT,
    operation        VARCHAR(32)                 NOT NULL,
    amount           DECIMAL                     NOT NULL,
    description      VARCHAR(50),
    from_wallet_iban VARCHAR(34),
    to_wallet_iban   VARCHAR(34),
    status           VARCHAR(20)                 NOT NULL,
    transaction_id   BIGINT,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    resolved_at      TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_spend_request PRIMARY KEY (id)
);

ALTER TABLE spend_request
    ADD CONSTRAINT fk_spend_request_on_organization FOREIGN KEY (organization_id) REFERENCES organization (id);

ALTER TABLE spend_request
    ADD CONSTRAINT fk_spend_request_on_requester FOREIGN KEY (requester_id) REFERENCES public."user" (id);

ALTER TABLE spend_request
    ADD CONSTRAINT fk_spend_request_on_approver FOREIGN KEY (approver_id) REFERENCES public."user" (id);

ALTER TABLE spend_request
    ADD CONSTRAINT fk_spend_request_on_transaction FOREIGN KEY (transaction_id) REFERENCES transaction (id);

CREATE INDEX idx_spend_request_org_status ON spend_request (organization_id, status);
