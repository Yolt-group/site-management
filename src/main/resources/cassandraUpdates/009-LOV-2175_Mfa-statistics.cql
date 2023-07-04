CREATE TABLE IF NOT EXISTS user_site_action (
    user_id uuid,
    user_site_id uuid,
    action_type text,
    exit_status text,
    exit_status_reason text,
    time timestamp,
    PRIMARY KEY (user_id, user_site_id, time)
) WITH CLUSTERING ORDER BY (user_site_id ASC, time DESC);
