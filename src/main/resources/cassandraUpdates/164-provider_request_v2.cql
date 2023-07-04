CREATE TABLE IF NOT EXISTS provider_request_v2 (
    id uuid,
    activity_id uuid,
    user_id uuid,
    user_site_id uuid,
    user_site_action_type text,
    PRIMARY KEY ((id), activity_id, user_id)
) WITH default_time_to_live = 3600;

CREATE MATERIALIZED VIEW IF NOT EXISTS provider_request_v2_by_user_activity AS
    SELECT * FROM provider_request_v2
    WHERE activity_id IS NOT NULL
    AND user_id IS NOT NULL
    AND id IS NOT NULL
    PRIMARY KEY ((user_id, activity_id), id);