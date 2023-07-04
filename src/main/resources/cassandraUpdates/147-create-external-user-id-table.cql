CREATE TABLE IF NOT EXISTS orphan_user_external_id (
    orphan_user_batch_id uuid,
    provider text,
    external_id text,
    PRIMARY KEY ((orphan_user_batch_id, provider), external_id)
);
