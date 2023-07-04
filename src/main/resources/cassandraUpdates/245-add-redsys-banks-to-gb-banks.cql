INSERT INTO popular_country_site_ranking (site_id, country_code, popular, ranking) VALUES (032b3544-925b-4b92-b84d-23e45d16a704, 'GB', false, NULL);
INSERT INTO popular_country_site_ranking (site_id, country_code, popular, ranking) VALUES (650d5187-6beb-4411-b107-69cea2891759, 'GB', false, NULL);
INSERT INTO popular_country_site_ranking (site_id, country_code, popular, ranking) VALUES (26c67b2b-91d3-491f-8121-b02fc9420101, 'GB', false, NULL);
INSERT INTO popular_country_site_ranking (site_id, country_code, popular, ranking) VALUES (bb5c3c49-f6c2-45c2-9067-855df78e6864, 'GB', false, NULL);

UPDATE site_v2 SET available_in_countries = ['ES', 'GB'] WHERE id = 032b3544-925b-4b92-b84d-23e45d16a704;
UPDATE site_v2 SET available_in_countries = ['ES', 'GB'] WHERE id = 650d5187-6beb-4411-b107-69cea2891759;
UPDATE site_v2 SET available_in_countries = ['ES', 'GB'] WHERE id = 26c67b2b-91d3-491f-8121-b02fc9420101;
UPDATE site_v2 SET available_in_countries = ['ES', 'GB'] WHERE id = bb5c3c49-f6c2-45c2-9067-855df78e6864;