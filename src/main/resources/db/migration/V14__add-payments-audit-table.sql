CREATE TYPE payment_event_t as enum ('DELETED');

CREATE CAST (varchar AS payment_event_t) WITH INOUT AS IMPLICIT;

CREATE TABLE IF NOT EXISTS payment_audit_log
(
    payment_id uuid            NOT NULL PRIMARY KEY,
    client_id  uuid            NOT NULL,
    user_id    uuid            NOT NULL,
    event      payment_event_t NOT NULL,
    metadata   json            NOT NULL,
    created_at timestamptz     NOT NULL
);

-- Add audit entry when a payment is deleted

CREATE OR REPLACE FUNCTION payment_audit_on_after_fn() RETURNS TRIGGER AS
$$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        INSERT INTO payment_audit_log(payment_id, client_id, user_id, event, metadata, created_at)
        VALUES (old.payment_id, old.client_id, old.user_id, 'DELETED'::payment_event_t, to_json(now()::timestamptz), now());

        RETURN old;
    ELSIF (TG_OP = 'INSERT') THEN
        RETURN new;
    ELSIF (TG_OP = 'UPDATE') THEN
        RETURN new;
    END IF;

    RETURN NULL; -- result is ignored since this is an AFTER trigger
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER payment_on_delete
    AFTER DELETE
    ON payment
    FOR EACH ROW
EXECUTE PROCEDURE payment_audit_on_after_fn();
