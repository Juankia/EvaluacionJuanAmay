-- Consumer idempotency (dedupe by eventId), per-subject ordering and delivery records.
CREATE TABLE processed_event (
    event_id      VARCHAR(64)  PRIMARY KEY,
    outcome       VARCHAR(32)  NOT NULL,
    processed_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE subject_state (
    source         VARCHAR(64)  NOT NULL,
    subject_id     VARCHAR(64)  NOT NULL,
    last_sequence  BIGINT       NOT NULL,
    PRIMARY KEY (source, subject_id)
);

CREATE TABLE delivery (
    id          BIGSERIAL    PRIMARY KEY,
    event_id    VARCHAR(64)  NOT NULL,
    channel     VARCHAR(8)   NOT NULL CHECK (channel IN ('PUSH', 'SMS')),
    status      VARCHAR(8)   NOT NULL CHECK (status IN ('SENT', 'FAILED')),
    attempts    INTEGER      NOT NULL,
    recorded_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX delivery_event_idx ON delivery (event_id);
