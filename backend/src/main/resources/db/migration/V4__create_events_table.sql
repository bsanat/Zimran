CREATE SEQUENCE s_events INCREMENT BY 1 MINVALUE 1 START 1;

-- Persistent event catalog. One row per provider event; the public id is '<provider>:<provider_event_id>'
-- (the numeric id never leaves the backend). Every value comes from the provider (or the demo seed, is_demo).
-- start_at is null for events the provider gives only a day (local_date) or free text (date_text);
-- starts_on / ends_on bound the days the event can happen on in its own timezone (null = open).
CREATE TABLE events
(
    id                BIGINT PRIMARY KEY,
    provider          VARCHAR(32)   NOT NULL,
    provider_event_id VARCHAR(255)  NOT NULL,
    title             TEXT          NOT NULL,
    description       TEXT,
    category_slug     VARCHAR(32)   NOT NULL,
    interest          VARCHAR(32)   NOT NULL,
    interests         VARCHAR(512)  NOT NULL,
    city              VARCHAR(100)  NOT NULL,
    state             VARCHAR(8),
    venue_name        TEXT,
    venue_address     TEXT,
    start_at          TIMESTAMPTZ,
    local_date        DATE,
    date_text         TEXT,
    schedule          TEXT,
    starts_on         DATE,
    ends_on           DATE,
    timezone          VARCHAR(64)   NOT NULL,
    price_min         NUMERIC,
    price_max         NUMERIC,
    currency          VARCHAR(3),
    price_known       BOOLEAN       NOT NULL DEFAULT FALSE,
    price_text        TEXT,
    status            VARCHAR(16)   NOT NULL,
    source_url        TEXT,
    image_url         TEXT,
    is_major          BOOLEAN       NOT NULL DEFAULT FALSE,
    is_demo           BOOLEAN       NOT NULL DEFAULT FALSE,
    first_seen_at     TIMESTAMPTZ   NOT NULL,
    fetched_at        TIMESTAMPTZ   NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ,
    deleted_at        TIMESTAMPTZ,
    is_deleted        BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_events_status CHECK (status IN ('ACTIVE', 'CANCELLED', 'POSTPONED', 'EXPIRED')),
    CONSTRAINT ck_events_price_known CHECK (price_known OR (price_min IS NULL AND price_max IS NULL))
);

CREATE UNIQUE INDEX ux_events_provider_event_id
    ON events (provider, provider_event_id) WHERE is_deleted = false;
CREATE INDEX ix_events_city_status ON events (lower(city), status) WHERE is_deleted = false;
CREATE INDEX ix_events_ends_on ON events (ends_on) WHERE is_deleted = false;
