DROP TRIGGER payment_on_delete ON payment;
DROP FUNCTION payment_audit_on_after_fn();
DROP TABLE payment_audit_log;
DROP TABLE payment;