-- enable vacuum at 1% of the total number of rows + 50 (threshold)
ALTER TABLE user_site
    SET (autovacuum_vacuum_scale_factor = 0.01, autovacuum_vacuum_threshold = 50);

-- enable vacuum at 1% of the total number of rows + 50 (threshold)
ALTER TABLE user_site_lock
    SET (autovacuum_vacuum_scale_factor = 0.01, autovacuum_vacuum_threshold = 50);
