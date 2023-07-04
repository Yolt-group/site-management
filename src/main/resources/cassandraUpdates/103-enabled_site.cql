CREATE TABLE IF NOT EXISTS enabled_site (
    client_id uuid,
    client_application_id uuid,
    site_id uuid,
    PRIMARY KEY (client_id, client_application_id, site_id)
);