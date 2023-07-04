CREATE TABLE IF NOT EXISTS enabled_provider (
    client_id uuid,
    client_application_id uuid,
    provider text,
    PRIMARY KEY ((client_id, client_application_id), provider)
);
