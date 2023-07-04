create type account_number_scheme_t as enum (
    'IBAN',
    'SORT_CODE_ACCOUNT_NUMBER'
);
create cast (varchar as account_number_scheme_t) with inout as implicit;

alter table payment alter column "provider_state" drop not null;

-- add a version column for optimistic locking and set all payment column versions on 0
alter table payment add column "version" bigint;
update payment set version = 0;
ALTER TABLE payment ALTER COLUMN version SET NOT NULL;
ALTER TABLE payment ALTER COLUMN version SET DEFAULT 0;

alter table payment add column "psu_ip_address" text;
alter table payment add column "dynamic_fields" json;
alter table payment add column "remittance_information_unstructured" text;
alter table payment add column "end_to_end_identifier" text;
alter table payment add column "payment_status_timestamp" timestamp;
alter table payment add column "amount" decimal;
alter table payment add column "currency" text;
alter table payment add column "creditor_name" text;
alter table payment add column "creditor_account_number" text;
alter table payment add column "creditor_account_number_scheme" account_number_scheme_t;
alter table payment add column "debtor_account_number" text;
alter table payment add column "debtor_account_number_scheme" account_number_scheme_t;

-- We should always update the payment status timestamp when we update the payment status field
CREATE OR REPLACE FUNCTION enforce_payment_status_timestamp_update_constraint_fn() RETURNS TRIGGER AS
$$
BEGIN
    IF (NEW.payment_status != OLD.payment_status AND NEW.payment_status_timestamp = OLD.payment_status_timestamp)
        OR (NEW.payment_status_timestamp != OLD.payment_status_timestamp AND NEW.payment_status = OLD.payment_status)
    THEN
        RAISE EXCEPTION 'The payment status timestamp should be updated when the payment status is updated.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER payment_status_update_constraint
    BEFORE UPDATE OF payment_status, payment_status_timestamp
    ON payment
    FOR EACH ROW
EXECUTE PROCEDURE enforce_payment_status_timestamp_update_constraint_fn();
