-- will replacer user_site_status_t
create type user_site_connection_status_t as enum (
    'CONNECTED',
    'STEP_NEEDED',
    'DISCONNECTED'
);
create cast (varchar as user_site_connection_status_t) with inout as implicit;

-- will replace user_site_reason_t
create type user_site_failure_reason_t as enum (
    'TECHNICAL_ERROR',
    'ACTION_NEEDED_AT_SITE',
    'AUTHENTICATION_FAILED',
    'CONSENT_EXPIRED'
);
create cast (varchar as user_site_failure_reason_t) with inout as implicit;

-- add new status & reason columns
alter table user_site add column connection_status user_site_connection_status_t;
alter table user_site add column failure_reason user_site_failure_reason_t;

-- The above is an initial step, later on we will execute the following additional steps:
-- fill connection_status and failure_reason for all rows (software)
-- mark them as non-nullable (sql)
-- drop the former 2 fields status, reason, and the associated enums (sql)
