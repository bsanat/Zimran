CREATE SEQUENCE s_pair_sessions INCREMENT BY 1 MINVALUE 1 START 1;

-- A Pair Planning session. public_id (pair_<random>) is the only id that leaves the backend; tokens are stored
-- as SHA-256 hashes only. conflict_type / conflict_relax are set with status NO_MATCH.
CREATE TABLE pair_sessions
(
    id                BIGINT PRIMARY KEY,
    public_id         VARCHAR(64)  NOT NULL,
    creator_scope_id  VARCHAR(64)  NOT NULL,
    invite_token_hash VARCHAR(64)  NOT NULL,
    status            VARCHAR(16)  NOT NULL,
    city              VARCHAR(100) NOT NULL,
    expires_at        TIMESTAMPTZ  NOT NULL,
    matched_event_id  VARCHAR(300),
    conflict_type     VARCHAR(16),
    conflict_relax    VARCHAR(16),
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ,
    deleted_at        TIMESTAMPTZ,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_pair_sessions_status
        CHECK (status IN ('WAITING_FOR_B', 'READY', 'MATCHED', 'NO_MATCH', 'EXPIRED', 'REVOKED')),
    CONSTRAINT ck_pair_sessions_conflict_type
        CHECK (conflict_type IS NULL OR conflict_type IN ('DATES', 'BUDGET', 'INTERESTS', 'VOTES')),
    CONSTRAINT ck_pair_sessions_conflict_relax
        CHECK (conflict_relax IS NULL OR conflict_relax IN ('DATES', 'BUDGET', 'INTERESTS'))
);

CREATE UNIQUE INDEX ux_pair_sessions_public_id ON pair_sessions (public_id) WHERE is_deleted = false;
CREATE UNIQUE INDEX ux_pair_sessions_invite_token_hash ON pair_sessions (invite_token_hash) WHERE is_deleted = false;
CREATE INDEX ix_pair_sessions_creator_scope_id ON pair_sessions (creator_scope_id) WHERE is_deleted = false;
