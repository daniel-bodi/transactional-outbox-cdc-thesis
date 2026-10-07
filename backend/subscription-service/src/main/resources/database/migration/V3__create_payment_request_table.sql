CREATE TABLE payment_request
(
    id              UUID           PRIMARY KEY,
    subscription_id UUID           NOT NULL REFERENCES subscription (id),
    amount          NUMERIC(19, 4) NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_request_subscription ON payment_request (subscription_id);
