CREATE TABLE IF NOT EXISTS user_site_delete(
    user_id uuid,
    user_site_id uuid,
    deleted timestamp,
    PRIMARY KEY (user_id, user_site_id)
);