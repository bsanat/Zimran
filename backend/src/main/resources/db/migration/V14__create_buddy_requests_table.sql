CREATE SEQUENCE s_buddy_requests INCREMENT BY 1 MINVALUE 1 START 1;

-- A request to join a Buddy post. public_id (breq_<random>) is the only id that leaves the backend. One request per
-- sender and post. resolved_at is set when it leaves PENDING.
CREATE TABLE buddy_requests
(
    id                  BIGINT PRIMARY KEY,
    public_id           VARCHAR(64)  NOT NULL,
    post_id             BIGINT       NOT NULL REFERENCES buddy_posts (id),
    sender_scope_id     VARCHAR(64)  NOT NULL,
    sender_display_name VARCHAR(50)  NOT NULL,
    message             VARCHAR(300) NOT NULL,
    status              VARCHAR(16)  NOT NULL,
    resolved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ,
    deleted_at          TIMESTAMPTZ,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_buddy_requests_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED'))
);

CREATE UNIQUE INDEX ux_buddy_requests_public_id ON buddy_requests (public_id) WHERE is_deleted = false;
CREATE UNIQUE INDEX ux_buddy_requests_post_sender ON buddy_requests (post_id, sender_scope_id) WHERE is_deleted = false;
CREATE INDEX ix_buddy_requests_sender ON buddy_requests (sender_scope_id) WHERE is_deleted = false;
