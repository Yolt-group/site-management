CREATE TABLE IF NOT EXISTS provider_request_v3 (
    user_id uuid,
    id uuid,
    activity_id uuid,
    user_site_id uuid,
    user_site_action_type text,
    PRIMARY KEY ((user_id), id)
) WITH default_time_to_live = 3600;