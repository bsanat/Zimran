CREATE SEQUENCE s_notification_deliveries INCREMENT BY 1 MINVALUE 1 START 1;

-- One row per channel a notification is delivered through. Only IN_APP exists now (DELIVERED on creation);
-- EMAIL (P2) adds rows here without schema changes.
CREATE TABLE notification_deliveries
(
    id              BIGINT PRIMARY KEY,
    notification_id BIGINT      NOT NULL REFERENCES notifications (id),
    channel         VARCHAR(16) NOT NULL,
    status          VARCHAR(16) NOT NULL,
    delivered_at    TIMESTAMPTZ,
    error           TEXT,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_notification_deliveries_channel CHECK (channel IN ('IN_APP', 'EMAIL')),
    CONSTRAINT ck_notification_deliveries_status CHECK (status IN ('PENDING', 'DELIVERED', 'FAILED'))
);

CREATE UNIQUE INDEX ux_notification_deliveries_notification_channel
    ON notification_deliveries (notification_id, channel) WHERE is_deleted = false;
