CREATE TABLE IF NOT EXISTS account_migration(
    user_id uuid,
    user_site_id_from uuid,
    account_external_id_from text,
    account_id_from uuid,
    site_id_from uuid,
    user_site_id_to uuid,
    account_external_id_to text,
    account_id_to uuid,
    site_id_to uuid,
    PRIMARY KEY ((user_id), user_site_id_from, account_external_id_from)
);