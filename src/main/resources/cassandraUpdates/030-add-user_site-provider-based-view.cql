CREATE MATERIALIZED VIEW IF NOT EXISTS user_site_by_provider AS
    SELECT * FROM user_site
    WHERE  user_id IS NOT NULL
    AND user_site_id is not null
    AND provider IS NOT NULL
    PRIMARY KEY (provider, user_id, user_site_id);