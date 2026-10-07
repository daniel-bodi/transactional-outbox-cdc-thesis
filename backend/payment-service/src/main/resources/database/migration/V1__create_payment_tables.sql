CREATE TABLE payment
(
    id                  UUID           PRIMARY KEY,
    reference           UUID           NOT NULL UNIQUE,
    amount              NUMERIC(19, 4) NOT NULL,
    status              VARCHAR(32)    NOT NULL,
    provider_reference  VARCHAR(255),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now()
);
