CREATE SEQUENCE s_pair_suggestions INCREMENT BY 1 MINVALUE 1 START 1;

-- Events suggested to both sides, computed once when B joins. compromise = the side whose interest the event
-- serves when it matches only one side's interests (null = an interest both share).
CREATE TABLE pair_suggestions
(
    id         BIGINT PRIMARY KEY,
    session_id BIGINT       NOT NULL REFERENCES pair_sessions (id),
    event_id   VARCHAR(300) NOT NULL,
    rank       INTEGER      NOT NULL,
    reason_a   TEXT         NOT NULL,
    reason_b   TEXT         NOT NULL,
    compromise VARCHAR(1),
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_pair_suggestions_rank CHECK (rank >= 1),
    CONSTRAINT ck_pair_suggestions_compromise CHECK (compromise IS NULL OR compromise IN ('A', 'B'))
);

CREATE UNIQUE INDEX ux_pair_suggestions_session_event
    ON pair_suggestions (session_id, event_id) WHERE is_deleted = false;
CREATE UNIQUE INDEX ux_pair_suggestions_session_rank
    ON pair_suggestions (session_id, rank) WHERE is_deleted = false;
