CREATE TABLE product_schema.p_outbox_events
(
    id                  UUID          NOT NULL PRIMARY KEY,
    event_id            UUID          NOT NULL,
    event_type          VARCHAR(100)  NOT NULL,
    aggregate_id        UUID          NOT NULL,
    deduplication_key   VARCHAR(200)  NOT NULL,
    trace_id            VARCHAR(100)  NOT NULL,
    payload             JSONB         NOT NULL,
    publish_status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    retry_count         INTEGER       NOT NULL DEFAULT 0,
    last_error           TEXT,
    published_at        TIMESTAMPTZ,
    created_at           TIMESTAMPTZ   NOT NULL,

    CONSTRAINT ck_outbox_events_publish_status
        CHECK (publish_status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_outbox_events_retry_count
        CHECK (retry_count >= 0),
    CONSTRAINT uq_outbox_events_event_id
        UNIQUE (event_id),
    CONSTRAINT uq_outbox_events_deduplication
        UNIQUE (event_type, aggregate_id, deduplication_key)
);

CREATE INDEX idx_outbox_events_pending_created_at
    ON product_schema.p_outbox_events (publish_status, created_at);
