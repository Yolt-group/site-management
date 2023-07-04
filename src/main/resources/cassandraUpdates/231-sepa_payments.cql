CREATE TABLE IF NOT EXISTS payment (
    client_id uuid,
    user_id uuid,
    payment_id uuid,
    external_payment_id text,
    redirect_url_id uuid,
    provider text,
    site_id uuid,
    updated timestamp,
    created timestamp,
    PRIMARY KEY ((client_id, user_id), payment_id)
);
ALTER TABLE payment_state ADD redirect_url_id uuid;
