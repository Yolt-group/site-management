DELETE FROM site WHERE country_code = 'UK' AND popular = false AND id = 82c16668-4d59-4be8-be91-1d52792f48e3;

INSERT INTO site
(country_code, popular, id, account_type_blacklist, account_type_whitelist, external_id, grouping_by, login_type, name, provider, ranking)
VALUES('GB', false, 82c16668-4d59-4be8-be91-1d52792f48e3, NULL, ['CURRENT'], NULL, NULL, 'URL', 'Monzo', 'MONZO', NULL);