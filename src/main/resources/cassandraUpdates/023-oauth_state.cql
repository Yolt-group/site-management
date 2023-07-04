
CREATE TABLE IF NOT EXISTS oauth_state (
    state text,
    site_id uuid,
    user_id uuid,
    PRIMARY KEY (state)
) with default_time_to_live=3600;
