CREATE TABLE IF NOT EXISTS site (
    id uuid,
    name text,
    country_code text,
    popular boolean,
    ranking int,
    external_id text,
    provider text,
    PRIMARY KEY (country_code, popular, id)
);

CREATE TABLE IF NOT EXISTS site_login_form (
    site_id uuid PRIMARY KEY,
    login_form_json text
);

CREATE TABLE IF NOT EXISTS user_site_mfa_form (
    user_site_id uuid PRIMARY KEY,
    mfa_form_json text
);

CREATE TABLE IF NOT EXISTS user_site (
    user_id uuid,
    user_site_id uuid,
    site_id uuid,
    external_id text,
    status text,
    reason text,
    status_timeout_time timestamp,
    created timestamp,
    updated timestamp,
    PRIMARY KEY (user_id, user_site_id)
);

CREATE TABLE IF NOT EXISTS batch_info (
    batch_id text,
    yodlee_to_time timestamp,
    PRIMARY KEY (batch_id)
);

CREATE TABLE IF NOT EXISTS yodlee_user_session (
    user_id uuid,
    yodlee_user_session_token text,
    created timestamp,
    marked_for_renew timestamp,
    PRIMARY KEY (user_id)
);
