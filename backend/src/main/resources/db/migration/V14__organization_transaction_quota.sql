-- Lifetime transaction subscription quota per organization (default 1000 for new and existing orgs).

ALTER TABLE organization
    ADD COLUMN transaction_quota BIGINT;

UPDATE organization
SET transaction_quota = 1000
WHERE transaction_quota IS NULL;

ALTER TABLE organization
    ALTER COLUMN transaction_quota SET NOT NULL;
