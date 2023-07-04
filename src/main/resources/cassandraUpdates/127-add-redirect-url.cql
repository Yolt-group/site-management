CREATE TABLE IF NOT EXISTS client_redirect_url (
client_id uuid,
redirect_url_id uuid,
base_redirect_url text,
updated timestamp,
PRIMARY KEY ((client_id), redirect_url_id)
);
