CREATE TABLE IF NOT EXISTS providers__client_application (
    client_id uuid,
    client_application_id uuid,
    base_redirect_url text,
    updated timestamp,
    PRIMARY KEY ((client_id), client_application_id)
);
