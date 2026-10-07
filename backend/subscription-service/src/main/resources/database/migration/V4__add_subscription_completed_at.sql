ALTER TABLE subscription
    ADD COLUMN completed_at TIMESTAMPTZ;

-- a settled subscription always carries its completion time, a pending one never does
ALTER TABLE subscription
    ADD CONSTRAINT chk_subscription_completed_at CHECK ((status = 'PENDING') = (completed_at IS NULL));
