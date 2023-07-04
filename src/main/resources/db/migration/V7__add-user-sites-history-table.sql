CREATE TYPE user_site_event_t as enum ('USER_SITE_DELETED');

CREATE CAST (varchar AS user_site_event_t) WITH INOUT AS IMPLICIT;

CREATE TABLE IF NOT EXISTS user_site_audit_log
(
    user_site_id uuid              NOT NULL,
    user_id      uuid              NOT NULL,
    event        user_site_event_t NOT NULL,
    metadata     json              NOT NULL,
    created_at   timestamptz       NOT NULL
);

CREATE INDEX CONCURRENTLY idx_user_id ON user_site_audit_log (user_id);


-- Add constraints on the delete attribute(s)

CREATE OR REPLACE FUNCTION enforce_deleted_constraints_fn() RETURNS TRIGGER AS
$$
BEGIN
    if NEW.is_deleted = true AND NEW.deleted_at IS NULL THEN
        RAISE EXCEPTION 'The deleted timestamp is required when a user-site is marked as deleted.';
    END IF;

    if NEW.is_deleted = false AND OLD.is_deleted = true THEN
        RAISE EXCEPTION 'The user-site cannot be un-marked as deleted.';
    end if;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER user_site_delete_constraints
    BEFORE UPDATE OF is_deleted, deleted_at
    ON user_site
    FOR EACH ROW
EXECUTE PROCEDURE enforce_deleted_constraints_fn();


-- Add audit entry when a user-site is marked as deleted

CREATE OR REPLACE FUNCTION audit_deleted_fn() RETURNS TRIGGER AS
$$
BEGIN
    if NEW.is_deleted = true AND OLD.is_deleted = false THEN
        INSERT INTO user_site_audit_log(user_site_id, user_id, event, metadata, created_at)
        VALUES (NEW.id, NEW.user_id, 'USER_SITE_DELETED'::user_site_event_t, to_json(now()::timestamptz), now());
    end if;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER user_site_audit_deleted
    BEFORE UPDATE OF is_deleted
    ON user_site
    FOR EACH ROW
EXECUTE PROCEDURE audit_deleted_fn();
