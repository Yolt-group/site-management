CREATE TABLE IF NOT EXISTS provider_request (
    id uuid,
    activity_id uuid,
    user_id uuid,
    user_site_id uuid,
    user_site_action_type text,
    PRIMARY KEY (id)
) WITH default_time_to_live = 3600;