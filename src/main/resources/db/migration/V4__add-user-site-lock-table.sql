CREATE TABLE IF NOT EXISTS user_site_lock
(
    user_site_id uuid        NOT NULL REFERENCES user_site (id) ON DELETE CASCADE,
    activity_id  uuid        NULL,
    locked_at    timestamptz NULL,
    PRIMARY KEY (user_site_id)
) WITH (fillfactor=90); -- a fillfactor of 90% leaves 10% free to enable in-page updates (Heap-Only Tuples). See https://github.com/postgres/postgres/blob/master/src/backend/access/heap/README.HOT

