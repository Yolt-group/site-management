DROP TABLE IF EXISTS orphan_user_external_id;
DROP TABLE IF EXISTS orphan_user_batch;
DROP TABLE IF EXISTS orphan_user;

CREATE TABLE IF NOT EXISTS orphan_user_external_id
(
    client_id            uuid,
    provider             text,
    orphan_user_batch_id uuid,
    external_id          text,
    PRIMARY KEY (client_id, provider, orphan_user_batch_id, external_id)
);

CREATE TABLE IF NOT EXISTS orphan_user
(
    client_id            uuid,
    provider             text,
    orphan_user_batch_id UUID,
    external_user_id     text,
    created_timestamp    timestamp,
    updated_timestamp    timestamp,
    status               text,
    PRIMARY KEY (client_id, provider, orphan_user_batch_id, external_user_id)
);

CREATE TABLE IF NOT EXISTS orphan_user_batch
(
    client_id            uuid,
    provider             text,
    orphan_user_batch_id UUID,
    created_timestamp    timestamp,
    updated_timestamp    timestamp,
    status               text,
    PRIMARY KEY (client_id, provider, orphan_user_batch_id)
);