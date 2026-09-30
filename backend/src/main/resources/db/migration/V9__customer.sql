-- SME customer / payee directory (org-scoped contacts with optional wallet link)

CREATE SEQUENCE IF NOT EXISTS customer_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE customer
(
    id                 BIGINT                      NOT NULL,
    organization_id    BIGINT                      NOT NULL,
    name               VARCHAR(100)                NOT NULL,
    phone              VARCHAR(20),
    email              VARCHAR(100),
    tax_id             VARCHAR(20),
    notes              VARCHAR(500),
    linked_wallet_id   BIGINT,
    created_by_user_id BIGINT                      NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status             VARCHAR(20)                 NOT NULL,
    CONSTRAINT pk_customer PRIMARY KEY (id)
);

ALTER TABLE customer
    ADD CONSTRAINT fk_customer_on_organization FOREIGN KEY (organization_id) REFERENCES organization (id);

ALTER TABLE customer
    ADD CONSTRAINT fk_customer_on_linked_wallet FOREIGN KEY (linked_wallet_id) REFERENCES wallet (id);

ALTER TABLE customer
    ADD CONSTRAINT fk_customer_on_created_by FOREIGN KEY (created_by_user_id) REFERENCES public."user" (id);

CREATE INDEX idx_customer_organization_id ON customer (organization_id);

CREATE INDEX idx_customer_org_name ON customer (organization_id, name);

CREATE UNIQUE INDEX uniq_customer_org_linked_wallet
    ON customer (organization_id, linked_wallet_id)
    WHERE linked_wallet_id IS NOT NULL;
