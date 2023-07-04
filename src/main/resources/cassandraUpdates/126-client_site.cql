CREATE TABLE IF NOT EXISTS client_site (
client_id uuid,
site_id uuid,
enabled boolean,
tags frozen<list<text>>,
PRIMARY KEY ((client_id), site_id)
);
