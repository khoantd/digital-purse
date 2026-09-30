-- Vietnam-first wallet: currency, double-entry ledger, idempotency keys

ALTER TABLE wallet
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'VND';

CREATE SEQUENCE IF NOT EXISTS ledger_entry_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE ledger_entry
(
    id             BIGINT                      NOT NULL,
    transaction_id BIGINT                      NOT NULL,
    wallet_id      BIGINT,
    account_code   VARCHAR(32)                 NOT NULL,
    entry_type     VARCHAR(6)                  NOT NULL,
    amount         DECIMAL                     NOT NULL,
    currency       VARCHAR(3)                  NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_ledger_entry PRIMARY KEY (id)
);

ALTER TABLE ledger_entry
    ADD CONSTRAINT FK_LEDGER_ENTRY_ON_TRANSACTION FOREIGN KEY (transaction_id) REFERENCES transaction (id);

ALTER TABLE ledger_entry
    ADD CONSTRAINT FK_LEDGER_ENTRY_ON_WALLET FOREIGN KEY (wallet_id) REFERENCES wallet (id);

CREATE INDEX idx_ledger_entry_transaction_id ON ledger_entry (transaction_id);
CREATE INDEX idx_ledger_entry_wallet_id ON ledger_entry (wallet_id);

CREATE SEQUENCE IF NOT EXISTS idempotency_record_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE idempotency_record
(
    id               BIGINT                      NOT NULL,
    user_id          BIGINT                      NOT NULL,
    operation        VARCHAR(32)                 NOT NULL,
    idempotency_key  VARCHAR(64)                 NOT NULL,
    response_id      BIGINT                      NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_idempotency_record PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uniq_idempotency_user_op_key
    ON idempotency_record (user_id, operation, idempotency_key);
