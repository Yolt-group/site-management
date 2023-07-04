CREATE MATERIALIZED VIEW IF NOT EXISTS client_redirect_url_by_base_url AS
    SELECT * FROM client_redirect_url
    WHERE base_redirect_url IS NOT NULL
    AND client_id IS NOT NULL
    AND redirect_url_id IS NOT NULL
    PRIMARY KEY ((base_redirect_url), client_id, redirect_url_id);