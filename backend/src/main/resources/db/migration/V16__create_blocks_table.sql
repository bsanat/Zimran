CREATE SEQUENCE s_blocks INCREMENT BY 1 MINVALUE 1 START 1;

-- One scope blocking another. A block hides both scopes' Buddy posts from each other and stops requests either way.
-- public_id (blk_<random>) is the scopeRef the blocker uses to lift it; scope ids never leave the backend.
CREATE TABLE blocks
(
    id               BIGINT PRIMARY KEY,
    public_id        VARCHAR(64) NOT NULL,
    blocker_scope_id VARCHAR(64) NOT NULL,
    blocked_scope_id VARCHAR(64) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ,
    deleted_at       TIMESTAMPTZ,
    is_deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_blocks_not_self CHECK (blocker_scope_id <> blocked_scope_id)
);

CREATE UNIQUE INDEX ux_blocks_public_id ON blocks (public_id) WHERE is_deleted = false;
CREATE UNIQUE INDEX ux_blocks_pair ON blocks (blocker_scope_id, blocked_scope_id) WHERE is_deleted = false;
CREATE INDEX ix_blocks_blocked ON blocks (blocked_scope_id) WHERE is_deleted = false;
