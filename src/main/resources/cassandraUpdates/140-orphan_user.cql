CREATE TABLE IF NOT EXISTS orphan_user (
    provider text,
    orphan_user_batch_id UUID,
    external_user_id text,
    created_timestamp timestamp,
    updated_timestamp timestamp,
    status text,
    PRIMARY KEY (provider, orphan_user_batch_id, external_user_id)
);

CREATE TABLE IF NOT EXISTS orphan_user_batch (
    provider text,
    orphan_user_batch_id UUID,
    created_timestamp timestamp,
    updated_timestamp timestamp,
    status text,
    PRIMARY KEY (provider, orphan_user_batch_id)
);