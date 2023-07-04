CREATE TABLE IF NOT EXISTS payment
(
    payment_id          uuid PRIMARY KEY,
    payment_status      text        NOT NULL,
    payment_type        text        NOT NULL,
    user_id             uuid        NOT NULL,
    client_id           uuid        NOT NULL,
    redirect_url_id     uuid        NOT NULL,
    site_id             uuid        NOT NULL,
    provider            text        NOT NULL,
    external_payment_id text        NULL,
    state               text        NOT NULL,
    provider_state      text        NOT NULL,
    submitted           bool        NOT NULL default false,
    updated_at          timestamptz NULL,
    created_at          timestamptz NOT NULL
);

CREATE INDEX idx_payment_user_id ON payment (user_id);
