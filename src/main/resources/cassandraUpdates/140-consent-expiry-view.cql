CREATE TABLE IF NOT EXISTS  external_consent_v2 (
user_id UUID,
site_id UUID,
user_site_id UUID,
consent_timestamp TIMESTAMP,
expiry_timestamp TIMESTAMP,
expiry_week TEXT,
external_consent_id TEXT,
PRIMARY KEY(user_id, site_id, user_site_id, expiry_week, expiry_timestamp)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS external_consent_by_expiry_time
AS SELECT *
FROM external_consent_v2
WHERE expiry_week IS NOT NULL
AND expiry_timestamp IS NOT NULL
AND user_id IS NOT NULL
AND site_id IS NOT NULL
AND user_site_id IS NOT NULL
PRIMARY KEY (expiry_week, expiry_timestamp, user_id, site_id, user_site_id);
