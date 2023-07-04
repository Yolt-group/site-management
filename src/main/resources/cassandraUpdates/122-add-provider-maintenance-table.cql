CREATE TABLE IF NOT EXISTS provider_maintenance (
    provider text PRIMARY KEY,
    manual_maintenance_from_utc timestamp,
    manual_maintenance_to_utc timestamp
);