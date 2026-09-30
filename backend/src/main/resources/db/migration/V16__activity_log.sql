-- Append-only org activity / security audit trail

CREATE SEQUENCE IF NOT EXISTS activity_log_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE activity_log
(
    id              BIGINT                      NOT NULL,
    organization_id BIGINT,
    actor_user_id   BIGINT,
    event_type      VARCHAR(64)                 NOT NULL,
    summary         VARCHAR(500)                NOT NULL,
    metadata_json   TEXT,
    ip_address      VARCHAR(64),
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_activity_log PRIMARY KEY (id)
);

ALTER TABLE activity_log
    ADD CONSTRAINT fk_activity_log_on_organization FOREIGN KEY (organization_id) REFERENCES organization (id);

ALTER TABLE activity_log
    ADD CONSTRAINT fk_activity_log_on_actor FOREIGN KEY (actor_user_id) REFERENCES public."user" (id);

CREATE INDEX idx_activity_log_org_created ON activity_log (organization_id, created_at DESC);

CREATE INDEX idx_activity_log_event_created ON activity_log (event_type, created_at DESC);

CREATE INDEX idx_activity_log_actor_created ON activity_log (actor_user_id, created_at DESC);
