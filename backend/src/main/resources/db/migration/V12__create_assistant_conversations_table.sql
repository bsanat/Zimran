CREATE SEQUENCE s_assistant_conversations INCREMENT BY 1 MINVALUE 1 START 1;

-- One AI assistant chat of a scope. The assistant quota is counted once, when the conversation starts; later
-- results in it count as turns, up to app.ai.max-conversation-turns, until expires_at. scope_id is a guest or
-- (P2) user scope, so it has no foreign key. public_id is the id the client sends back.
CREATE TABLE assistant_conversations
(
    id         BIGINT PRIMARY KEY,
    public_id  VARCHAR(36) NOT NULL,
    scope_id   VARCHAR(64) NOT NULL,
    turns      INTEGER     NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_assistant_conversations_turns CHECK (turns >= 0)
);

CREATE UNIQUE INDEX ux_assistant_conversations_public_id ON assistant_conversations (public_id) WHERE is_deleted = false;
CREATE INDEX ix_assistant_conversations_scope_id ON assistant_conversations (scope_id) WHERE is_deleted = false;
