/* Previous 'Yolt Bank (form)', it's the same as e278a008-bf45-4d19-bb5d-b36ff755be58*/
DELETE FROM site_v2 where id = f5639ec9-df4d-473c-a665-2edd8ddefae8;
DELETE FROM site_login_form where site_id = f5639ec9-df4d-473c-a665-2edd8ddefae8;
DELETE FROM site_consent_template where site_id = f5639ec9-df4d-473c-a665-2edd8ddefae8;

/* Perf Test Bank 1 - Persona 18, no longer used*/
DELETE FROM site_v2 where id = c3162db0-62f6-4623-997c-15fa970a082c;
DELETE FROM site_login_form where site_id = c3162db0-62f6-4623-997c-15fa970a082c;
DELETE FROM site_consent_template where site_id = c3162db0-62f6-4623-997c-15fa970a082c;

/* Refers to a test bank hosted by yodlee.*/
UPDATE site_v2 SET name = 'Yodlee test bank' WHERE id = e278a008-bf45-4d19-bb5d-b36ff755be58;

/* 2 banks, same as the one above. Goes to yodlee test bank, but belong in a 'migration group' together with OB test impl.*/
UPDATE site_v2 SET name = 'Migration group yodlee test bank (migrated)' WHERE id = 6dbddacf-8195-46c1-b57e-c0beeea49990;
UPDATE site_v2 SET name = 'Migration group yodlee test bank (migration)' WHERE id = 4f958fc6-220e-11e8-b467-0ed5f89f718b;

/* It uses the YODLEE dataprovider, but yoltbank stubs the actual yodlee. */
UPDATE site_v2 SET name = 'Yoltbank-Yodlee flow' WHERE id = 8d25dc30-7dc9-11e8-adc0-fa7ae01bbebc;
UPDATE site_v2 SET name = 'Yoltbank-Budget insight flow' WHERE id = 0285a320-7dca-11e8-adc0-fa7ae01bbebc;
UPDATE site_v2 SET name = 'Yoltbank-Saltedge flow' WHERE id = 082af309-8f16-452c-a48e-0a8830b603b7;

/* Just here for clarity of having all the test banks in a list. Only changed casing to be consistent*/
UPDATE site_v2 SET name = 'Yolt test Open banking' WHERE id = ca8a362a-a351-4358-9f1c-a8b4b91ed65b;

/* The new/renamed 'real' test bank. */
UPDATE site_v2 SET name = 'Yolt test bank' WHERE id = 33aca8b9-281a-4259-8492-1b37706af6db;
