CREATE TABLE IF NOT EXISTS site_consent_template(
site_id uuid,
id timeuuid,
template text,
PRIMARY KEY (site_id, id)
)   WITH CLUSTERING ORDER BY (id DESC);