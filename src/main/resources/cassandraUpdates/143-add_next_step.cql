ALTER TABLE user_session ADD form_step text;
ALTER TABLE user_session ADD redirect_url_step text;
ALTER TABLE user_session WITH  default_time_to_live = 3600