CREATE MATERIALIZED VIEW IF NOT EXISTS client_application_by_redirect_url AS
    SELECT * FROM client_application
    WHERE base_redirect_url IS NOT NULL
    AND client_id IS NOT NULL
    AND client_application_id IS NOT NULL
    PRIMARY KEY ((base_redirect_url), client_id, client_application_id);