alter table user_site
alter column migration_status set default 'NONE';

drop trigger user_site_default_values_trigger on user_site;

drop function user_site_defaults;

alter table user_site
drop column status,
drop column reason;

-- cascade drops the associated casts from/to character varying
drop type user_site_status_t cascade;
drop type user_site_reason_t cascade;
