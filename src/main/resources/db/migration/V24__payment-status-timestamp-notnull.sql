UPDATE payment
SET payment_status_timestamp = COALESCE(updated_at, created_at) -- created_at is not null so this should always work.
WHERE payment_status_timestamp IS NULL;

ALTER TABLE payment
    ALTER COLUMN payment_status_timestamp SET NOT NULL;
