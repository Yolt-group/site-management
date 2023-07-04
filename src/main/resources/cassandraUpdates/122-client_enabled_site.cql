CREATE TABLE IF NOT EXISTS client_enabled_site (
    client_id uuid,
    site_id uuid,
    PRIMARY KEY ((client_id), site_id)
);