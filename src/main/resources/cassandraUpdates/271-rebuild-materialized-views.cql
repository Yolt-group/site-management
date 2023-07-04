/* original: client_redirect_url_by_base_url */
CREATE MATERIALIZED VIEW IF NOT EXISTS client_redirect_url_by_base_url_v2 AS
SELECT *
FROM client_redirect_url
WHERE base_redirect_url IS NOT NULL
  AND client_id IS NOT NULL
  AND redirect_url_id IS NOT NULL PRIMARY KEY ((base_redirect_url), client_id, redirect_url_id);

/* original: external_consent_by_expiry_time_v2 */
CREATE MATERIALIZED VIEW external_consent_by_expiry_time_v3
AS
SELECT *
FROM external_consent_v3
WHERE expiry_week IS NOT NULL
  AND user_id IS NOT NULL
  AND site_id IS NOT NULL
  AND user_site_id IS NOT NULL PRIMARY KEY (expiry_week, user_id, site_id, user_site_id);

/* original: external_id_user */
CREATE MATERIALIZED VIEW IF NOT EXISTS external_id_user_v2 AS
SELECT *
FROM user_external_id
WHERE user_id IS NOT NULL
  AND provider IS NOT NULL
  AND external_user_id IS NOT NULL PRIMARY KEY (external_user_id, provider, user_id);

/* original: provider_request_v2_by_user_activity */
CREATE MATERIALIZED VIEW IF NOT EXISTS provider_request_v2_by_user_activity_v2 AS
SELECT *
FROM provider_request_v2
WHERE activity_id IS NOT NULL
  AND user_id IS NOT NULL
  AND id IS NOT NULL PRIMARY KEY ((user_id, activity_id), id);

/* original: user_site_by_site_v2 */
CREATE MATERIALIZED VIEW IF NOT EXISTS user_site_by_site_v3 AS
SELECT *
FROM user_site
WHERE user_id IS NOT NULL
  AND user_site_id IS NOT NULL
  AND site_id IS NOT NULL PRIMARY KEY (site_id, user_id, user_site_id);

/* original: user_site_session_by_state_id */
CREATE MATERIALIZED VIEW IF NOT EXISTS user_site_session_by_state_id_v2 AS
SELECT state_id, user_id, user_site_id, client_id, redirect_url_id
FROM user_site_session
WHERE state_id IS NOT NULL
  AND user_id IS NOT NULL
  AND user_site_id IS NOT NULL
  AND client_id IS NOT NULL
  AND redirect_url_id IS NOT NULL PRIMARY KEY ((state_id), user_id, user_site_id);
