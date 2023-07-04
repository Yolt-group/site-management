CREATE TABLE IF NOT EXISTS daily_added_user_site(
  site_id uuid,
  user_id uuid,
  user_site_id uuid,
  PRIMARY KEY (site_id, user_id, user_site_id)
);