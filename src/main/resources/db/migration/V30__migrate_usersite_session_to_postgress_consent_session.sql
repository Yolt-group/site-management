create type consent_operation as enum (
    'UPDATE_USER_SITE',
    'CREATE_USER_SITE'
    );
create cast (varchar as consent_operation) with inout as implicit;

CREATE TABLE consent_session
(
    user_id                    uuid              NOT NULL,
    user_site_id               uuid              NOT NULL,
    state_id                   uuid              NOT NULL UNIQUE,
    activity_id                uuid              NOT NULL,
    client_id                  uuid              NOT NULL,
    created                    timestamptz       NOT NULL,
    provider                   text              NOT NULL,
    site_id                    uuid              NOT NULL,
    operation                  consent_operation NOT NULL,
    step_number                int               NOT NULL default 0,
    external_consent_id        text,
    original_connection_status user_site_connection_status_t,
    original_failure_reason    user_site_failure_reason_t,
    provider_state             text,
    redirect_url_id            uuid,
    form_step                  text,
    redirect_url_step          text,
    primary key (user_id, user_site_id)
);