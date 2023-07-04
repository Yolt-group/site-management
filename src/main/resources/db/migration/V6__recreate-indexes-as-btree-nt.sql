-- BEWARE: This migration is ran WITHOUT a transaction
-- By default flyway runs all migrations in a separate transaction, but CREATE INDEX CONCURRENTLY
-- cannot be ran inside a transaction.
-- Flyway will automatically detect the presence of such statements and will not start a transaction.
---
-- *DO NOT MIX TRANSACTIONAL AND NON TRANSACTIONAL IN A SINGLE MIGRATION*

-- Recreate Btree/Bin indexes as hash
--
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_btree_site_id ON user_site (site_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_btree_user_id ON user_site (user_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_btree_client_id ON user_site (client_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_btree_status ON user_site (status);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_btree_migration_status ON user_site (migration_status);

-- Drop previous BTREE/Bin indexes
--
DROP INDEX CONCURRENTLY IF EXISTS idx_site_id; -- btree
DROP INDEX CONCURRENTLY IF EXISTS idx_user_id; -- btree
DROP INDEX CONCURRENTLY IF EXISTS idx_client_id; -- btree
DROP INDEX CONCURRENTLY IF EXISTS idx_ldf_brin; -- brin
