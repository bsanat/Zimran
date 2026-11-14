CREATE SEQUENCE s_alert_preferences INCREMENT BY 1 MINVALUE 1 START 1;

-- Alert preferences of one scope (Premium / demo). scope_id is a guest or (P2) user scope, so it has no foreign key.
-- city is a supported city id; category_slugs and channels hold enum names. enabled_at is set when alerts are first
-- enabled or un-paused: only events first seen at or after it can match.
CREATE TABLE alert_preferences
(
    id             BIGINT PRIMARY KEY,
    scope_id       VARCHAR(64)  NOT NULL,
    scope_kind     VARCHAR(16)  NOT NULL,
    city           VARCHAR(100) NOT NULL,
    category_slugs TEXT[]       NOT NULL,
    budget_max     NUMERIC,
    frequency      VARCHAR(16)  NOT NULL,
    channels       TEXT[]       NOT NULL,
    paused         BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ,
    deleted_at     TIMESTAMPTZ,
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_alert_preferences_scope_kind CHECK (scope_kind IN ('GUEST', 'USER')),
    CONSTRAINT ck_alert_preferences_frequency CHECK (frequency IN ('INSTANT', 'DAILY', 'WEEKLY')),
    CONSTRAINT ck_alert_preferences_budget_max CHECK (budget_max IS NULL OR budget_max >= 0)
);

CREATE UNIQUE INDEX ux_alert_preferences_scope_id ON alert_preferences (scope_id) WHERE is_deleted = false;
CREATE INDEX ix_alert_preferences_city_active ON alert_preferences (city)
    WHERE is_deleted = false AND paused = false AND enabled_at IS NOT NULL;
