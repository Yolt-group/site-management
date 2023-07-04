CREATE TABLE IF NOT EXISTS user_external_id (
    user_id uuid,
    provider text,
    external_user_id text,
    PRIMARY KEY (user_id, provider)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS external_id_user AS
    SELECT * FROM user_external_id
    WHERE user_id IS NOT NULL
    AND provider IS NOT NULL
    AND external_user_id IS NOT NULL
    PRIMARY KEY (external_user_id, provider, user_id);
