CREATE TABLE charge
(
    id         UUID           PRIMARY KEY,
    reference  UUID           NOT NULL,
    amount     NUMERIC(19, 4) NOT NULL,
    status     VARCHAR(32)    NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now()
);
