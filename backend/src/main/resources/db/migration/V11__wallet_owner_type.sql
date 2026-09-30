-- Wallet ownership label (organization vs customer); tenancy remains organization_id

ALTER TABLE wallet
    ADD COLUMN owner_type VARCHAR(20) NOT NULL DEFAULT 'ORGANIZATION';

ALTER TABLE wallet
    ADD COLUMN customer_id BIGINT;

ALTER TABLE wallet
    ADD CONSTRAINT fk_wallet_on_customer FOREIGN KEY (customer_id) REFERENCES customer (id);

ALTER TABLE wallet
    ADD CONSTRAINT chk_wallet_owner_type CHECK (
        (owner_type = 'ORGANIZATION' AND customer_id IS NULL)
            OR (owner_type = 'CUSTOMER' AND customer_id IS NOT NULL)
        );

CREATE INDEX idx_wallet_customer_id ON wallet (customer_id);
