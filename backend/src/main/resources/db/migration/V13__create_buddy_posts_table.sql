CREATE SEQUENCE s_buddy_posts INCREMENT BY 1 MINVALUE 1 START 1;

-- An Event Buddy post: "I am going to this event, who wants to join?". public_id (bud_<random>) is the only id that
-- leaves the backend; owner_scope_id is a guest or (P2) user scope, so it has no foreign key. event_id is the public
-- event id (json:..., demo:...). expires_at is the start of the event's next occurrence (end of that day for an
-- event without a time); the expiry job then sets EXPIRED. No contact data is stored.
CREATE TABLE buddy_posts
(
    id             BIGINT PRIMARY KEY,
    public_id      VARCHAR(64)  NOT NULL,
    event_id       VARCHAR(300) NOT NULL,
    owner_scope_id VARCHAR(64)  NOT NULL,
    display_name   VARCHAR(50)  NOT NULL,
    short_bio      VARCHAR(300) NOT NULL,
    language       VARCHAR(32),
    preferences    VARCHAR(300),
    age_min        INTEGER,
    age_max        INTEGER,
    status         VARCHAR(16)  NOT NULL,
    expires_at     TIMESTAMPTZ  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ,
    deleted_at     TIMESTAMPTZ,
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_buddy_posts_status CHECK (status IN ('OPEN', 'MATCHED', 'CLOSED', 'EXPIRED')),
    CONSTRAINT ck_buddy_posts_age_min CHECK (age_min IS NULL OR age_min >= 18),
    CONSTRAINT ck_buddy_posts_age_max CHECK (age_max IS NULL OR age_max >= 18),
    CONSTRAINT ck_buddy_posts_age_range CHECK (age_min IS NULL OR age_max IS NULL OR age_min <= age_max)
);

CREATE UNIQUE INDEX ux_buddy_posts_public_id ON buddy_posts (public_id) WHERE is_deleted = false;
-- At most one OPEN post per scope and event.
CREATE UNIQUE INDEX ux_buddy_posts_owner_event_open ON buddy_posts (owner_scope_id, event_id)
    WHERE is_deleted = false AND status = 'OPEN';
CREATE INDEX ix_buddy_posts_event_open ON buddy_posts (event_id, expires_at)
    WHERE is_deleted = false AND status = 'OPEN';
CREATE INDEX ix_buddy_posts_owner ON buddy_posts (owner_scope_id) WHERE is_deleted = false;
CREATE INDEX ix_buddy_posts_expiring ON buddy_posts (expires_at)
    WHERE is_deleted = false AND status IN ('OPEN', 'MATCHED');
