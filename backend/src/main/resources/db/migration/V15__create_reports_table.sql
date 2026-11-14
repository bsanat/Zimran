CREATE SEQUENCE s_reports INCREMENT BY 1 MINVALUE 1 START 1;

-- A user report for moderation. target_id is the public id the reporter named (a Buddy post or request; for SCOPE
-- the post or request whose author is reported). reported_scope_id is resolved on the server and never returned.
CREATE TABLE reports
(
    id                BIGINT PRIMARY KEY,
    reporter_scope_id VARCHAR(64) NOT NULL,
    target_type       VARCHAR(16) NOT NULL,
    target_id         VARCHAR(64) NOT NULL,
    reported_scope_id VARCHAR(64) NOT NULL,
    reason            VARCHAR(32) NOT NULL,
    comment           VARCHAR(500),
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ,
    deleted_at        TIMESTAMPTZ,
    is_deleted        BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_reports_target_type CHECK (target_type IN ('POST', 'REQUEST', 'SCOPE')),
    CONSTRAINT ck_reports_reason
        CHECK (reason IN ('SPAM', 'HARASSMENT', 'INAPPROPRIATE', 'SCAM', 'UNDERAGE', 'CONTACT_DETAILS', 'OTHER'))
);

CREATE INDEX ix_reports_reported_scope ON reports (reported_scope_id) WHERE is_deleted = false;
