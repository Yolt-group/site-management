CREATE TABLE IF NOT EXISTS user_count_by_client (
    client_id uuid,
    user_count counter,
    PRIMARY KEY (client_id)
);
