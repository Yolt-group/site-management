CREATE TABLE IF NOT EXISTS site_v2 (
    id uuid,
    name text,
    external_id text,
    provider text,
    login_type text,
    account_type_whitelist list<text>,
    grouping_by text,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS popular_country_site_ranking (
    site_id uuid,
    country_code text,
    popular boolean,
    ranking int,
    PRIMARY KEY (site_id, country_code)
);