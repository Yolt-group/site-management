CREATE TABLE IF NOT EXISTS site_migration_group(
  site_id uuid,
  migrate_to_id uuid,
  convert_to_id uuid,
  PRIMARY KEY (site_id, migrate_to_id)
);
