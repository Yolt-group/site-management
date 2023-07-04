ALTER TABLE user_site
    ADD COLUMN is_deleted bool NOT NULL DEFAULT false,
    ADD COLUMN deleted_at timestamptz DEFAULT NULL ;
