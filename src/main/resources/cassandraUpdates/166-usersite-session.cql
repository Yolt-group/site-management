CREATE TABLE IF NOT EXISTS user_site_session(
  user_id uuid,
  user_site_id uuid,
  state_id text,
  site_id uuid,
  provider text,
  uses_dynamic_steps Boolean,
  operation text,
  redirect_url_id uuid,
  provider_state text,
  migrating_user_site_id uuid,
  external_consent_id text,
  form_step text,
  redirect_url_step text,
  activity_id uuid,
  PRIMARY KEY ((user_id), user_site_id)
) with default_time_to_live=3600;

CREATE TABLE IF NOT EXISTS generated_session_states (
  user_id uuid,
  state_id text,
  submitted Boolean,
  created timestamp,
  submitted_time timestamp,
  PRIMARY KEY ((user_id), state_id)
) with default_time_to_live=31536000;
