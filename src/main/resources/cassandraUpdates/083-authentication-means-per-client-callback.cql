ALTER TABLE user_site ADD client_application_id uuid;

CREATE TABLE IF NOT EXISTS client_application (
    client_id uuid,
    client_application_id uuid,
    base_redirect_url text,
    updated timestamp,
    PRIMARY KEY ((client_id), client_application_id)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS client_application_by_redirect_url AS
    SELECT * FROM client_application
    WHERE base_redirect_url IS NOT NULL
    AND client_id IS NOT NULL
    AND client_application_id IS NOT NULL
    PRIMARY KEY ((base_redirect_url), client_id, client_application_id);

CREATE TABLE IF NOT EXISTS client_application_provider_authentication_means (
    client_id uuid,
    client_application_id uuid,
    provider text,
    authentication_means text,
    updated timestamp,
    PRIMARY KEY ((client_id, client_application_id), provider)
);


ALTER TABLE login_state ADD site_id uuid;
ALTER TABLE login_state ADD provider text;
