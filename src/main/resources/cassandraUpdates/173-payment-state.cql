CREATE TABLE IF NOT EXISTS payment_state(
  user_id uuid,
  state_id text,
  site_id uuid,
  provider text,
  provider_state text,
  submitted boolean,
  PRIMARY KEY ((user_id), state_id)
);