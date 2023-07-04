CREATE TABLE IF NOT EXISTS user_refresh_batch (
    refresh_minute_utc text,
    client_id uuid,
    user_ids frozen<set<uuid>>,
    PRIMARY KEY (refresh_minute_utc, client_id)
) WITH default_time_to_live = 86399;
