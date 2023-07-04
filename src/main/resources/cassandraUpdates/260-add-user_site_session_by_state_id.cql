CREATE MATERIALIZED VIEW IF NOT EXISTS user_site_session_by_state_id AS
SELECT state_id, user_id, user_site_id, client_id, redirect_url_id
FROM user_site_session
WHERE state_id IS NOT NULL
  AND user_id IS NOT NULL
  AND user_site_id IS NOT NULL
  AND client_id IS NOT NULL
  AND redirect_url_id IS NOT NULL
PRIMARY KEY ((state_id), user_id, user_site_id);
