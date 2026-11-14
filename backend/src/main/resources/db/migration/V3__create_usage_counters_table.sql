CREATE SEQUENCE s_usage_counters INCREMENT BY 1 MINVALUE 1 START 1;

-- One row per scope, feature and quota window (window_start is aligned; 1970-01-01 = a one-time trial).
CREATE TABLE usage_counters
(
    id           BIGINT PRIMARY KEY,
    scope_id     VARCHAR(64)  NOT NULL,
    feature      VARCHAR(32)  NOT NULL,
    window_start TIMESTAMPTZ  NOT NULL,
    count        INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ,
    deleted_at   TIMESTAMPTZ,
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_usage_counters_count CHECK (count >= 0)
);

CREATE UNIQUE INDEX ux_usage_counters_scope_feature_window
    ON usage_counters (scope_id, feature, window_start) WHERE is_deleted = false;
