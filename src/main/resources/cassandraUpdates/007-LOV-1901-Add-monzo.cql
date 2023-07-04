CREATE TABLE IF NOT EXISTS oauth_token (
    user_id uuid,
    provider text,
    state text,
    access_token text,
    expire_time timestamp,
    refresh_token text,
    updated timestamp,
    PRIMARY KEY (user_id, provider)
);
