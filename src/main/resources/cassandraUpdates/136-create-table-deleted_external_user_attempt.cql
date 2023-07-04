CREATE TABLE IF NOT EXISTS deleted_external_user_attempt (
    provider text,
    deleted timestamp,
    external_user_id text,
    internal_user_id uuid,
    PRIMARY KEY (provider, deleted, external_user_id, internal_user_id)
);