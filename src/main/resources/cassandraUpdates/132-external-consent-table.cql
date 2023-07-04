CREATE TABLE IF NOT EXISTS  external_consent (
  user_id UUID,
  site_id UUID,
  user_site_id UUID,
  consent_timestamp TIMESTAMP,
  expiry_timestamp TIMESTAMP,
  expiry_month TEXT,
  external_consent_id TEXT,
  PRIMARY KEY(user_id, site_id, user_site_id, expiry_month, expiry_timestamp)
);

ALTER TABLE site_v2 ADD consent_expiry_in_days INT;
ALTER TABLE login_state ADD external_consent_id TEXT;

UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = 36130c5f-9024-4a89-91fc-be31fac2f9ec;
UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = 6ace4a0c-c335-4a23-bfe2-33f95d22621e;
UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = ac75d67d-5ede-4972-94a8-3b8481fa2145;
UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = d28b4598-efcf-41c8-8522-08b2744e551a;
UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = 5fe1e9f8-eb5f-4812-a6a6-2002759db545;
UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = 8b21aab6-e0a3-43ae-be5e-def71509bef0;
UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = 1ed6cc15-60fe-4b00-adf8-bd8e04e7804e;
UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = 71b4ad6b-d620-4049-9f09-f9fd9110bd15;
UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = 654435c8-0c86-46e7-89b6-f2bd7a9d7ebc;
UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = 8e96ad29-fd1c-40cb-b28f-826b022f2763;
UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = 217a893a-5040-4291-a6be-85790c8b65c6;
UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = dda2651f-9518-409b-9b5e-6e0d1ccd0112;
UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = fd4ee7f7-dca1-45ff-a687-40119fee8c65;
UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = 3651edaa-d36e-48cb-8cc3-94bb1fbe8f76;
UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = 5eac330b-a345-45d5-a84b-c5381321b7eb;
UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = 5806ae85-7ee6-48a5-98ea-0f464b9b71cb;
UPDATE site_v2 SET consent_expiry_in_days = 90 WHERE id = 9416de84-502b-4522-a622-635a02ed8924;