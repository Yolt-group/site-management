-- set the pre-existing end_to_end identifiers to non null value
update payment set end_to_end_identifier = concat('fake_', substring(md5(random()::text), 0, 30)) where end_to_end_identifier is null;

alter table payment alter column end_to_end_identifier set not null;

-- add the unique constraint on the end-to-end identifier and client id columns so we can guarantee the clients are only able to set the end-to-end-identifier once
alter table payment add unique(end_to_end_identifier, client_id);
