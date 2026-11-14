CREATE SEQUENCE s_guest_scopes INCREMENT BY 1 MINVALUE 1 START 1;

CREATE TABLE guest_scopes
(
    id            BIGINT PRIMARY KEY,
    scope_id      VARCHAR(64)  NOT NULL,
    age_confirmed BOOLEAN      NOT NULL DEFAULT FALSE,
    display_name  VARCHAR(100),
    last_seen_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ,
    deleted_at    TIMESTAMPTZ,
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX ux_guest_scopes_scope_id ON guest_scopes (scope_id) WHERE is_deleted = false;
