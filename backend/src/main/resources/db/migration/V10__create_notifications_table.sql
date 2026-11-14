CREATE SEQUENCE s_notifications INCREMENT BY 1 MINVALUE 1 START 1;

-- In-app notifications. public_id (notif_<random>) is the only id that leaves the backend. The title is rendered
-- on read from title_key + title_args (i18n). A notification is visible from deliver_after on; batch_key names the
-- DAILY / WEEKLY delivery window (null for INSTANT). One notification per (scope, event, kind): re-ingesting an
-- event never creates a second one (insert ... on conflict do nothing).
CREATE TABLE notifications
(
    id            BIGINT PRIMARY KEY,
    public_id     VARCHAR(64)  NOT NULL,
    scope_id      VARCHAR(64)  NOT NULL,
    event_id      BIGINT       NOT NULL REFERENCES events (id),
    kind          VARCHAR(32)  NOT NULL,
    title_key     VARCHAR(100) NOT NULL,
    title_args    TEXT[]       NOT NULL,
    category_slug VARCHAR(32)  NOT NULL,
    deliver_after TIMESTAMPTZ  NOT NULL,
    read_at       TIMESTAMPTZ,
    batch_key     VARCHAR(64),
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ,
    deleted_at    TIMESTAMPTZ,
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_notifications_kind CHECK (kind IN ('NEW_EVENT_MATCH'))
);

CREATE UNIQUE INDEX ux_notifications_public_id ON notifications (public_id) WHERE is_deleted = false;
CREATE UNIQUE INDEX ux_notifications_scope_event_kind
    ON notifications (scope_id, event_id, kind) WHERE is_deleted = false;
CREATE INDEX ix_notifications_scope_deliver_after
    ON notifications (scope_id, deliver_after DESC) WHERE is_deleted = false;
