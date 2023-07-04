CREATE TABLE IF NOT EXISTS user_session(
  user_id uuid,
  site_id uuid,
  state_id text,
  redirect_url_id uuid,
  provider_state text,
  provider text,
  migrating_user_site_id uuid,
  external_consent_id text,
  PRIMARY KEY (user_id, site_id)
);