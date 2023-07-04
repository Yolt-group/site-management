CREATE TABLE IF NOT EXISTS yodlee_clean_up_user_site (
    external_user_id uuid,
    external_user_site_id text,
    deleted timestamp,
    PRIMARY KEY (external_user_id, external_user_site_id)
);

CREATE TABLE IF NOT EXISTS yodlee_user_site (
    external_user_id text,
    external_user_site_id text,
    PRIMARY KEY (external_user_id, external_user_site_id)
);