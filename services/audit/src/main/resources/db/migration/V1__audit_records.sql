-- Local audit persistence (ADR-0020). The per-source hash chain DETECTS accidental or naive
-- modification; it is NOT tamper-resistant storage and does NOT meet regulatory retention.
CREATE TABLE audit_record (
    position        BIGSERIAL    PRIMARY KEY,
    event_id        VARCHAR(64)  NOT NULL UNIQUE,
    event_type      VARCHAR(100) NOT NULL,
    schema_version  INTEGER      NOT NULL,
    source          VARCHAR(64)  NOT NULL,
    subject_type    VARCHAR(32)  NOT NULL,
    subject_id      VARCHAR(64)  NOT NULL,
    sequence        BIGINT       NOT NULL,
    audit_category  VARCHAR(2)   NOT NULL CHECK (audit_category IN ('A1','A2','A3','A4','A5','A6','A7','A8')),
    occurred_at     TIMESTAMPTZ  NOT NULL,
    recorded_at     TIMESTAMPTZ  NOT NULL,
    correlation_id  VARCHAR(64),
    payload_json    TEXT         NOT NULL, -- exact bytes hashed in the chain (JSONB would normalise them)
    sequence_gap    BOOLEAN      NOT NULL,
    previous_hash   CHAR(64)     NOT NULL,
    chain_hash      CHAR(64)     NOT NULL
);

CREATE INDEX audit_record_subject_idx ON audit_record (subject_id, occurred_at DESC);
CREATE INDEX audit_record_source_subject_seq_idx ON audit_record (source, subject_id, sequence);

CREATE TABLE audit_chain_head (
    source          VARCHAR(64)  PRIMARY KEY,
    last_hash       CHAR(64)     NOT NULL,
    record_count    BIGINT       NOT NULL
);
