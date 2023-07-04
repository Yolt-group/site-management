alter table payment add column deleted bool NOT NULL DEFAULT false;
alter table payment add column deleted_at timestamptz NULL;
