CREATE TABLE IF NOT EXISTS processed_events
(
    consumer_group VARCHAR(255) NOT NULL,
    event_id       UUID         NOT NULL,
    processed_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer_group, event_id)
);
