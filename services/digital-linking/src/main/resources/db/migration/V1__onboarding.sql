-- Onboarding requests (safe subset: E1 only). Optimistic concurrency by version (CAS).
CREATE TABLE onboarding_request (
    request_id              UUID         PRIMARY KEY,
    identification_digest   CHAR(64)     NOT NULL, -- keyed SHA-256 of the identification; never stored in clear
    state                   VARCHAR(48)  NOT NULL,
    version                 BIGINT       NOT NULL,
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL
);

-- IC-15: at most one non-terminal request per identification.
CREATE UNIQUE INDEX onboarding_request_active_identification_uq
    ON onboarding_request (identification_digest)
    WHERE state NOT IN ('IDENTITY_REJECTED');

-- IC-01/IC-08/IC-09: one record per (scope, key) with the request fingerprint.
CREATE TABLE idempotency_record (
    scope         VARCHAR(80)  NOT NULL,
    idem_key      VARCHAR(128) NOT NULL,
    fingerprint   CHAR(64)     NOT NULL,
    request_id    UUID         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (scope, idem_key)
);

-- Transactional outbox (ADR-0015): the event is written in the same commit as the transition.
CREATE TABLE outbox_event (
    event_id      VARCHAR(64)  PRIMARY KEY,
    event_type    VARCHAR(100) NOT NULL,
    body          TEXT         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    published_at  TIMESTAMPTZ
);

CREATE INDEX outbox_event_pending_idx ON outbox_event (created_at) WHERE published_at IS NULL;
