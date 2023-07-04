CREATE TABLE IF NOT EXISTS user_site_access_means (
    user_id uuid,
    user_site_id uuid,
    provider text,
    access_means text,
    updated timestamp,
    expire_time timestamp,
    PRIMARY KEY (user_id, user_site_id, provider)
);
