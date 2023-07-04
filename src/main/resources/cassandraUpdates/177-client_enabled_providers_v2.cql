CREATE TABLE IF NOT EXISTS client_enabled_provider_v2 (
    client_id uuid,
    provider text,
    service_type text,
    PRIMARY KEY ((client_id), provider, service_type)
);

CREATE TABLE IF NOT EXISTS client_redirect_url_enabled_provider_v2 (
    client_id uuid,
    redirect_url_id uuid,
    provider text,
    service_type text,
    PRIMARY KEY ((client_id), redirect_url_id, provider, service_type)
);

ALTER TABLE site_v2 ADD sepa_single text;

UPDATE site_v2 SET available_in_countries = ['GB'] WHERE id = 8f0cc956-8c1d-11e9-bc42-526af7764f64;
UPDATE site_v2 SET available_in_countries = ['IT'] WHERE id = 35270590-680b-4448-a65f-dc976f7986ad;
UPDATE site_v2 SET available_in_countries = ['IT'] WHERE id = 5d51472e-86e6-4dfc-9f61-85845d2ac3d6;
UPDATE site_v2 SET available_in_countries = ['GB'] WHERE id = fc472090-8388-11e9-bc42-526af7764f64;
UPDATE site_v2 SET available_in_countries = ['NL'] WHERE id = 2967f2c0-f0e6-4f1f-aeba-e4357b82ca7a;
UPDATE site_v2 SET available_in_countries = ['NL', 'BE'] WHERE id = 52417433-dabd-40fa-84e3-9fcb650bb087;
UPDATE site_v2 SET available_in_countries = ['PL', 'GB'] WHERE id = c8d4f2f8-dced-432f-8507-638b9a7968f8;
UPDATE site_v2 SET available_in_countries = ['IT'] WHERE id = 889cebad-56d2-4369-b2f0-3b424fbd0f9f;
UPDATE site_v2 SET available_in_countries = ['NL', 'BE'] WHERE id = c173ad24-30cf-4007-bff1-dfcc4d4989eb;
UPDATE site_v2 SET available_in_countries = ['PL'] WHERE id = 70f8fc37-179e-4ab1-a992-756fab08ff3d;
UPDATE site_v2 SET available_in_countries = ['NL', 'BE'] WHERE id = 85930b0b-58db-4312-a4db-7bdfb13faae1;
UPDATE site_v2 SET available_in_countries = ['GB'] WHERE id = c9a0fcdb-b505-4a21-99cf-ffffca5189db;
UPDATE site_v2 SET available_in_countries = ['AT','AU','CZ','DE', 'LU','NL','PL','RO','TH'] WHERE id = 44bbc1b2-029e-11e9-8eb2-f2801f1b9fd1;
UPDATE site_v2 SET available_in_countries = ['IT'] WHERE id = b7855573-7178-48fa-a85c-7aac3f9d3ef2;

UPDATE site_v2 SET uses_step_types = {'AIS':['FORM']}, services = ['AIS'] WHERE id = 35270590-680b-4448-a65f-dc976f7986ad;
UPDATE site_v2 SET uses_step_types = {'AIS':['FORM']}, services = ['AIS'] WHERE id = 5d51472e-86e6-4dfc-9f61-85845d2ac3d6;
UPDATE site_v2 SET uses_step_types = {'AIS':['REDIRECT']}, services = ['AIS'] WHERE id = 2967f2c0-f0e6-4f1f-aeba-e4357b82ca7a;
UPDATE site_v2 SET uses_step_types = {'AIS':['FORM']}, services = ['AIS'] WHERE id = 52417433-dabd-40fa-84e3-9fcb650bb087;
UPDATE site_v2 SET uses_step_types = {'AIS':['REDIRECT']}, services = ['AIS'] WHERE id = c8d4f2f8-dced-432f-8507-638b9a7968f8;
UPDATE site_v2 SET uses_step_types = {'AIS':['FORM']}, services = ['AIS'] WHERE id =  889cebad-56d2-4369-b2f0-3b424fbd0f9f;
UPDATE site_v2 SET uses_step_types = {'AIS':['FORM']}, services = ['AIS'] WHERE id = c173ad24-30cf-4007-bff1-dfcc4d4989eb;
UPDATE site_v2 SET uses_step_types = {'AIS':['REDIRECT']}, services = ['AIS'] WHERE id = 70f8fc37-179e-4ab1-a992-756fab08ff3d;
UPDATE site_v2 SET uses_step_types = {'AIS':['FORM']}, services = ['AIS'] WHERE id = 85930b0b-58db-4312-a4db-7bdfb13faae1;
UPDATE site_v2 SET uses_step_types = {'AIS':['FORM']}, services = ['AIS'] WHERE id = c9a0fcdb-b505-4a21-99cf-ffffca5189db;
UPDATE site_v2 SET uses_step_types = {'AIS':['FORM']}, services = ['AIS'] WHERE id = 44bbc1b2-029e-11e9-8eb2-f2801f1b9fd1;
UPDATE site_v2 SET uses_step_types = {'AIS':['FORM']}, services = ['AIS'] WHERE id = b7855573-7178-48fa-a85c-7aac3f9d3ef2;