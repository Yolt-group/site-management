ALTER TABLE site_v2 ADD uses_step_types frozen<map<text,frozen<list<text>>>>;
ALTER TABLE site_v2 ADD available_in_countries frozen<list<text>>;