ALTER TABLE user_site ADD flow_version text;
ALTER TABLE user_session WITH  default_time_to_live = 3600;