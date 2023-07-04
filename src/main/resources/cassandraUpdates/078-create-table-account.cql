CREATE TABLE IF NOT EXISTS account (
  user_id uuid,
  user_site_id uuid,
  account_id uuid,
  external_id text,
  provider text,
  created timestamp,
  updated timestamp,
  last_data_fetch timestamp,
  last_transaction_id text,
  PRIMARY KEY ((user_id), user_site_id, account_id)
);
