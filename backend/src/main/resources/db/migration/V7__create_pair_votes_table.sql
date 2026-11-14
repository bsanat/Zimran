CREATE SEQUENCE s_pair_votes INCREMENT BY 1 MINVALUE 1 START 1;

-- A side's vote on one suggested event (public event id); a new vote replaces the old one.
CREATE TABLE pair_votes
(
    id         BIGINT PRIMARY KEY,
    session_id BIGINT       NOT NULL REFERENCES pair_sessions (id),
    role       VARCHAR(1)   NOT NULL,
    event_id   VARCHAR(300) NOT NULL,
    vote       VARCHAR(8)   NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_pair_votes_role CHECK (role IN ('A', 'B')),
    CONSTRAINT ck_pair_votes_vote CHECK (vote IN ('LIKE', 'MAYBE', 'PASS'))
);

CREATE UNIQUE INDEX ux_pair_votes_session_role_event
    ON pair_votes (session_id, role, event_id) WHERE is_deleted = false;
