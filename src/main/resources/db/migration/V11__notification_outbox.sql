CREATE TABLE notification_outbox (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id   UUID        NOT NULL UNIQUE,
    type       VARCHAR(40) NOT NULL,
    payload    JSONB       NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_pending ON notification_outbox (id) WHERE sent_at IS NULL;
