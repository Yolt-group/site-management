CREATE TABLE IF NOT EXISTS login_state (
    state text,
    user_id uuid,
    PRIMARY KEY (state)
) with default_time_to_live=3600;

CREATE TABLE IF NOT EXISTS access_means (
    user_id uuid,
    provider text,
    access_means text,
    updated timestamp,
    expire_time timestamp,
    PRIMARY KEY (user_id, provider)
);