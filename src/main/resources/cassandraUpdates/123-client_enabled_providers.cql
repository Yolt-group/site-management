CREATE TABLE IF NOT EXISTS client_enabled_provider (
    client_id uuid,
    provider text,
    PRIMARY KEY ((client_id), provider)
);

CREATE TABLE IF NOT EXISTS client_redirect_url_enabled_provider (
    client_id uuid,
    redirect_url_id uuid,
    provider text,
    PRIMARY KEY ((client_id), redirect_url_id, provider)
);
