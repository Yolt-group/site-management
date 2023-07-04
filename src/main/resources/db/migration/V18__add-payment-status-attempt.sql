alter table payment add column payment_status_attempt integer NOT NULL default 0;

-- We should always zero the payment status attempt when we update the payment status field
CREATE OR REPLACE FUNCTION enforce_payment_status_attempt_update_constraint_fn() RETURNS TRIGGER AS
$$
BEGIN
    IF (NEW.payment_status != OLD.payment_status AND NEW.payment_status_attempt != 0)
        OR (NEW.payment_status = OLD.payment_status AND NEW.payment_status_attempt < OLD.payment_status_attempt)
    THEN
        RAISE EXCEPTION 'The payment status attempt should be updated when the payment status is updated.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER payment_status_attempt_update_constraint
    BEFORE UPDATE OF payment_status, payment_status_attempt
    ON payment
    FOR EACH ROW
EXECUTE PROCEDURE enforce_payment_status_attempt_update_constraint_fn();
