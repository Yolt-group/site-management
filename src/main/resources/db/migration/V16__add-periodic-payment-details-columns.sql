CREATE TYPE frequency_t as enum (
    'DAILY',
    'WEEKLY',
    'MONTHLY',
    'YEARLY');

CREATE CAST (varchar AS frequency_t) WITH INOUT AS IMPLICIT;

alter table payment add column frequency frequency_t NULL;
alter table payment add column periodic_start_date timestamptz NULL;
alter table payment add column periodic_end_date timestamptz NULL;
