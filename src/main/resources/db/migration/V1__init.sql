-- create user_site_status_t enum w/ type conversion (varchar -> user_site_status_t)

CREATE TYPE user_site_status_t as enum (
    'INITIAL_PROCESSING',
    'STEP_NEEDED',
    'STEP_TIMED_OUT',
    'STEP_FAILED',
    'LOGIN_SUCCEEDED',
    'LOGIN_FAILED',
    'REFRESH_TRIGGERED',
    'REFRESH_FINISHED',
    'REFRESH_FAILED',
    'REFRESH_TIMED_OUT',
    'REFRESH_IN_PROGRESS', -- todo: appears to be unused
    'MFA_FAILED',
    'MFA_NEEDED',
    'MFA_TIMED_OUT',
    'DELETING',
    'FAILED_TO_DELETE',
    'UNKNOWN');

CREATE CAST (varchar AS user_site_status_t) WITH INOUT AS IMPLICIT;

-- create user_site_reason_t enum w/ type conversion (varchar -> user_site_reason_t)

CREATE TYPE user_site_reason_t as enum (
    'INCORRECT_CREDENTIALS',
    'EXPIRED_CREDENTIALS',
    'INCORRECT_ANSWER',
    'SITE_ERROR',
    'GENERIC_ERROR',
    'MULTIPLE_LOGINS',
    'SITE_ACTION_NEEDED',
    'UNSUPPORTED_LANGUAGE',
    'NEW_LOGIN_INFO_NEEDED',
    'MFA_TIMED_OUT',
    'STEP_TIMED_OUT',
    'WRONG_CREDENTIALS',
    'TOKEN_EXPIRED',
    'MFA_NOT_SUPPORTED',
    'STEP_NOT_SUPPORTED',
    'NO_SUPPORTED_ACCOUNTS',
    'CONSENT_EXPIRED',
    'ACCOUNT_LOCKED');

CREATE CAST (varchar AS user_site_reason_t) WITH INOUT AS IMPLICIT;


-- create migration_status_t enum w/ type conversion (varchar -> migration_status_t)

create type migration_status_t as enum (
    'MIGRATING_TO',
    'MIGRATING_FROM',
    'MIGRATION_NEEDED',
    'NONE',
    'MIGRATION_IN_PROGRESS',
    'MIGRATION_DONE');

CREATE CAST (varchar AS migration_status_t) WITH INOUT AS IMPLICIT;


CREATE TABLE IF NOT EXISTS user_site
(
    id                          uuid PRIMARY KEY, -- primary
    user_id                     uuid               NOT NULL,
    site_id                     uuid               NOT NULL,
    client_id                   uuid               NOT NULL,
    external_id                 text               NULL,
    status                      user_site_status_t NOT NULL default 'UNKNOWN',
    reason                      user_site_reason_t NULL,
    status_timeout_time         timestamptz        NULL,
    created                     timestamptz        NOT NULL,
    updated                     timestamptz        NULL,
    last_data_fetch             timestamptz        NULL,
    provider                    text               NOT NULL default 'YODLEE',
    migration_status            migration_status_t NULL     default 'NONE',
    redirect_url_id             uuid               NULL,
    persisted_form_step_answers json               NULL,
    version                     bigint             NOT NULL default 0
);

-- CREATE CAST (varchar AS timestamptz) WITH INOUT AS ASSIGNMENT;
-- CREATE CAST (varchar AS json) WITH INOUT AS ASSIGNMENT;

CREATE INDEX idx_user_id ON user_site (user_id);
CREATE INDEX idx_site_id ON user_site (site_id);
CREATE INDEX idx_client_id ON user_site (client_id); -- todo: partial index?

CREATE INDEX idx_ldf_brin ON user_site USING BRIN (last_data_fetch); -- BRIN is cheap

CREATE OR REPLACE FUNCTION user_site_defaults() RETURNS TRIGGER AS
$$
BEGIN
    NEW.status := coalesce(NEW.status, 'UNKNOWN');
    NEW.provider := coalesce(NEW.provider, 'YODLEE');
    NEW.migration_status := coalesce(NEW.migration_status, 'NONE');
    NEW.created := coalesce(NEW.created, '1970-01-01 00:00:00.000');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER user_site_default_values_trigger
    BEFORE INSERT OR UPDATE
    ON user_site
    FOR EACH ROW
EXECUTE PROCEDURE user_site_defaults();
