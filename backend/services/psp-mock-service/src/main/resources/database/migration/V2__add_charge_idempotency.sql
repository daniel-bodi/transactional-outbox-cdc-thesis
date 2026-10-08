-- only filled when the mock honours idempotency keys; NULLs do not collide in a unique constraint
ALTER TABLE charge ADD COLUMN idempotency_key VARCHAR(255) UNIQUE;

-- duplicate requests with an already seen idempotency key, answered without a new charge
ALTER TABLE charge ADD COLUMN duplicate_requests INTEGER NOT NULL DEFAULT 0;
