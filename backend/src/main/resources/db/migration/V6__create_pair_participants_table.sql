CREATE SEQUENCE s_pair_participants INCREMENT BY 1 MINVALUE 1 START 1;

-- One row per side (A = creator, B = invitee). interests are interest ids (music, food, ...).
CREATE TABLE pair_participants
(
    id                BIGINT PRIMARY KEY,
    session_id        BIGINT       NOT NULL REFERENCES pair_sessions (id),
    role              VARCHAR(1)   NOT NULL,
    scope_id          VARCHAR(64)  NOT NULL,
    access_token_hash VARCHAR(64)  NOT NULL,
    interests         TEXT[]       NOT NULL,
    budget_max        NUMERIC,
    date_from         DATE         NOT NULL,
    date_to           DATE         NOT NULL,
    time_preference   VARCHAR(16),
    message           TEXT,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ,
    deleted_at        TIMESTAMPTZ,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_pair_participants_role CHECK (role IN ('A', 'B')),
    CONSTRAINT ck_pair_participants_budget CHECK (budget_max IS NULL OR budget_max >= 0),
    CONSTRAINT ck_pair_participants_dates CHECK (date_to >= date_from)
);

CREATE UNIQUE INDEX ux_pair_participants_session_role
    ON pair_participants (session_id, role) WHERE is_deleted = false;
