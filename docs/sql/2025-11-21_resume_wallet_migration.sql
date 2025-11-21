-- Resume migration script for wallet UUID
-- Date: 2025-11-21
-- Purpose: Continue migration from where it failed
-- USE THIS IF: You already ran steps 1-3 and have wallet_id column with UUIDs

-- STEP 0: Check current state
SELECT 'Checking current state...' AS info;
DESCRIBE wallets;

-- Verify wallet_id column exists and has UUIDs
SELECT
    CASE
        WHEN COUNT(*) = (SELECT COUNT(*) FROM wallets WHERE wallet_id IS NOT NULL)
        THEN 'OK: All wallets have UUIDs'
        ELSE 'ERROR: Some wallets missing UUIDs'
    END AS uuid_status,
    COUNT(*) AS total_wallets,
    COUNT(CASE WHEN wallet_id IS NOT NULL THEN 1 END) AS wallets_with_uuid
FROM wallets;

-- ============================================================================
-- STEP 1: Prepare dependent tables (wallet_transactions) to switch to UUID FK
-- ============================================================================
SELECT 'Preparing wallet_transactions to use UUID foreign key...' AS info;

-- 1.1 Add staging column for UUID mapping if not exists
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_transactions' AND COLUMN_NAME = 'wallet_id_uuid'
);
SET @sql_add_stage := IF(@col_exists = 0,
    'ALTER TABLE wallet_transactions ADD COLUMN wallet_id_uuid VARCHAR(36) NULL AFTER wallet_id',
    'SELECT "wallet_id_uuid already exists, skipping" AS info');
PREPARE stmt FROM @sql_add_stage; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 Map old BIGINT wallet_id -> new UUID using wallets.id -> wallets.wallet_id
UPDATE wallet_transactions wt
JOIN wallets w ON wt.wallet_id = w.id
SET wt.wallet_id_uuid = w.wallet_id
WHERE wt.wallet_id_uuid IS NULL;
SELECT CONCAT('Mapped ', ROW_COUNT(), ' wallet_transactions to UUID staging column') AS status;

-- 1.3 Drop FK from wallet_transactions -> wallets.id (dynamic name)
SET @fk_tx := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wallet_transactions'
      AND COLUMN_NAME = 'wallet_id'
      AND REFERENCED_TABLE_NAME = 'wallets'
      AND REFERENCED_COLUMN_NAME = 'id'
    LIMIT 1
);
SET @sql_drop_fk_tx := IF(@fk_tx IS NOT NULL,
    CONCAT('ALTER TABLE wallet_transactions DROP FOREIGN KEY ', @fk_tx),
    'SELECT "No FK from wallet_transactions to wallets.id found, skipping" AS info');
PREPARE stmt FROM @sql_drop_fk_tx; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.4 Ensure index on wallet_transactions.wallet_id is dropped if it blocks type change
SET @idx_tx := (
    SELECT INDEX_NAME FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_transactions' AND COLUMN_NAME = 'wallet_id' AND INDEX_NAME <> 'PRIMARY' LIMIT 1
);
SET @sql_drop_idx_tx := IF(@idx_tx IS NOT NULL,
    CONCAT('DROP INDEX ', @idx_tx, ' ON wallet_transactions'),
    'SELECT "No secondary index on wallet_transactions.wallet_id to drop" AS info');
PREPARE stmt FROM @sql_drop_idx_tx; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.5 Change wallet_transactions.wallet_id to VARCHAR(36)
-- Note: After this ALTER, wallet_id in wallet_transactions is VARCHAR(36) but still has BIGINT values
ALTER TABLE wallet_transactions MODIFY COLUMN wallet_id VARCHAR(36) NULL;

-- 1.6 Copy UUIDs from staging column and drop staging
-- Note: wallet_id_uuid was created in step 1.1 above
-- IDE may show warning but column exists at runtime
UPDATE wallet_transactions SET wallet_id = wallet_id_uuid WHERE wallet_id_uuid IS NOT NULL;
ALTER TABLE wallet_transactions DROP COLUMN wallet_id_uuid;
SELECT 'wallet_transactions now uses VARCHAR(36) wallet_id values' AS status;

-- ============================================================================
-- STEP 2: Switch wallets PK from id (BIGINT) to wallet_id (UUID)
-- ============================================================================

-- 2.1 Remove AUTO_INCREMENT from id column (required before dropping PK)
ALTER TABLE wallets MODIFY COLUMN id BIGINT NOT NULL;
SELECT 'Removed AUTO_INCREMENT from wallets.id' AS status;

-- 2.2 Drop old primary key
ALTER TABLE wallets DROP PRIMARY KEY;
SELECT 'Dropped old primary key on wallets.id' AS status;

-- 2.3 Drop old id column
ALTER TABLE wallets DROP COLUMN id;
SELECT 'Dropped wallets.id column' AS status;

-- 2.4 Make wallet_id the new NOT NULL PK
ALTER TABLE wallets MODIFY COLUMN wallet_id VARCHAR(36) NOT NULL;
ALTER TABLE wallets ADD PRIMARY KEY (wallet_id);
SELECT 'wallet_id is now PRIMARY KEY' AS status;

-- 2.5 Verify wallets table structure
DESCRIBE wallets;

-- ============================================================================
-- STEP 3: Drivers linking and constraints (safe re-run)
-- ============================================================================
-- STEP 6: Add wallet_id column to drivers table (if not exists)
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'drivers'
    AND COLUMN_NAME = 'wallet_id'
);

SET @sql_add_col = IF(@column_exists = 0,
    'ALTER TABLE drivers ADD COLUMN wallet_id VARCHAR(36) NULL AFTER plan_id',
    'SELECT "Column wallet_id already exists, skipping" AS info');
PREPARE stmt FROM @sql_add_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Checked/Added wallet_id column to drivers' AS status;

-- STEP 7: Link existing drivers to their wallets
-- Note: drivers.wallet_id was created in step 3.6 above, and wallets.wallet_id is now the PRIMARY KEY
-- IDE may show warning but columns exist at runtime
UPDATE drivers d
INNER JOIN wallets w ON d.user_id = w.user_id
SET d.wallet_id = w.wallet_id
WHERE d.wallet_id IS NULL OR d.wallet_id != w.wallet_id;
SELECT CONCAT('Linked ', ROW_COUNT(), ' drivers to their wallets') AS status;

-- STEP 8: Add foreign key constraint (if not exists)
SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'drivers'
    AND CONSTRAINT_NAME = 'fk_driver_wallet'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @sql_add_fk = IF(@fk_exists = 0,
    'ALTER TABLE drivers ADD CONSTRAINT fk_driver_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id) ON DELETE SET NULL',
    'SELECT "Foreign key fk_driver_wallet already exists, skipping" AS info');
PREPARE stmt FROM @sql_add_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Checked/Added foreign key constraint' AS status;

-- STEP 9: Add index for better performance (if not exists)
SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'drivers'
    AND INDEX_NAME = 'idx_drivers_wallet_id'
);

SET @sql_add_idx = IF(@idx_exists = 0,
    'CREATE INDEX idx_drivers_wallet_id ON drivers(wallet_id)',
    'SELECT "Index idx_drivers_wallet_id already exists, skipping" AS info');
PREPARE stmt FROM @sql_add_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Checked/Created index on drivers.wallet_id' AS status;

-- STEP 10: Verify migration results
SELECT
    'Total wallets' AS description,
    COUNT(*) AS count
FROM wallets
UNION ALL
SELECT
    'Drivers with wallet linked' AS description,
    COUNT(*) AS count
FROM drivers
WHERE wallet_id IS NOT NULL
UNION ALL
SELECT
    'Drivers without wallet' AS description,
    COUNT(*) AS count
FROM drivers
WHERE wallet_id IS NULL;

-- Show sample data
SELECT
    d.user_id,
    u.email,
    u.first_name,
    u.last_name,
    d.wallet_id,
    w.balance,
    w.updated_at
FROM drivers d
INNER JOIN users u ON d.user_id = u.user_id
LEFT JOIN wallets w ON d.wallet_id = w.wallet_id
LIMIT 10;

SELECT '✅ Migration completed successfully!' AS result;
