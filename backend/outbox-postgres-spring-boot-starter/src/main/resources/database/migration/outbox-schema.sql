CREATE TABLE IF NOT EXISTS outbox
(
    id             UUID         PRIMARY KEY,
    trace_id       VARCHAR(255),
    aggregate_id   VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    type           VARCHAR(255) NOT NULL,
    payload        JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
