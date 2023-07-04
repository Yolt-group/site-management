CREATE TABLE IF NOT EXISTS client_group_enabled_provider (
    client_group_id uuid,
    provider text,
    service_type text,
    PRIMARY KEY ((client_group_id), provider, service_type)
);