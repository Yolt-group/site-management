CREATE TABLE IF NOT EXISTS site_consent(
  user_id uuid,
  site_id uuid,
  template_id timeuuid,
  approved timestamp,
  PRIMARY KEY (user_id, site_id)
);