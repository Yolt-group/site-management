CREATE TABLE IF NOT EXISTS user_site_lock (
    user_id uuid,
    user_site_id uuid,
    PRIMARY KEY (user_id, user_site_id)
) WITH default_time_to_live = 600;
