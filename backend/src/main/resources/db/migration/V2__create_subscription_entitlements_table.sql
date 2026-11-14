CREATE SEQUENCE s_subscription_entitlements INCREMENT BY 1 MINVALUE 1 START 1;

-- scope_id is a guest or (P2) user scope, so it has no foreign key. No row = FREE.
CREATE TABLE subscription_entitlements
(
    id                       BIGINT PRIMARY KEY,
    scope_id                 VARCHAR(64)  NOT NULL,
    status                   VARCHAR(32)  NOT NULL,
    provider                 VARCHAR(32),
    provider_subscription_id VARCHAR(255),
    period_end               TIMESTAMPTZ,
    demo_only                BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ,
    deleted_at               TIMESTAMPTZ,
    is_deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_subscription_entitlements_status CHECK (status IN ('FREE', 'DEMO_PREMIUM', 'PREMIUM_ACTIVE'))
);

CREATE UNIQUE INDEX ux_subscription_entitlements_scope_id
    ON subscription_entitlements (scope_id) WHERE is_deleted = false;
