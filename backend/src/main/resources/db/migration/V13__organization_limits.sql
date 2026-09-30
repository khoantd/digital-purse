-- Per-organization transactional limits (VND). Platform app.limits remain defaults for new orgs.

ALTER TABLE organization
    ADD COLUMN per_transaction_max     NUMERIC(19, 0),
    ADD COLUMN daily_outbound_max      NUMERIC(19, 0),
    ADD COLUMN daily_top_up_max        NUMERIC(19, 0),
    ADD COLUMN dual_control_threshold  NUMERIC(19, 0);

UPDATE organization
SET per_transaction_max    = 50000000,
    daily_outbound_max     = 100000000,
    daily_top_up_max       = 100000000,
    dual_control_threshold = 10000000
WHERE per_transaction_max IS NULL;

ALTER TABLE organization
    ALTER COLUMN per_transaction_max SET NOT NULL,
    ALTER COLUMN daily_outbound_max SET NOT NULL,
    ALTER COLUMN daily_top_up_max SET NOT NULL,
    ALTER COLUMN dual_control_threshold SET NOT NULL;
