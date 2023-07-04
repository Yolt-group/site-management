DROP MATERIALIZED VIEW IF EXISTS user_site_by_site;

ALTER TABLE user_site drop client_application_id;

CREATE MATERIALIZED VIEW IF NOT EXISTS user_site_by_site_v2 AS
    SELECT * FROM user_site
    WHERE user_id IS NOT NULL
    AND user_site_id IS NOT NULL
    AND site_id IS NOT NULL
    PRIMARY KEY (site_id, user_id, user_site_id);