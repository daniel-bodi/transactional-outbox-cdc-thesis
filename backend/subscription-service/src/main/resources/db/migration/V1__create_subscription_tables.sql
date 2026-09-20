CREATE TABLE plan
(
    id     UUID           PRIMARY KEY,
    name   VARCHAR(64)    NOT NULL UNIQUE,
    amount NUMERIC(19, 4) NOT NULL
);

CREATE TABLE subscription
(
    id          UUID        PRIMARY KEY,
    customer_id UUID        NOT NULL,
    plan_id     UUID        NOT NULL REFERENCES plan (id),
    status      VARCHAR(32) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_subscription_plan ON subscription (plan_id);
