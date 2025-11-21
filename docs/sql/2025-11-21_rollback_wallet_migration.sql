-- Rollback script for wallet UUID migration
-- Date: 2025-11-21
-- Purpose: Restore wallets table to original BIGINT AUTO_INCREMENT structure
-- USE ONLY IF: Migration failed or you need to revert changes

-- WARNING: This will restore wallets from backup table
-- Make sure wallets_backup exists before running!

-- STEP 1: Verify backup exists
SELECT
    CASE
        WHEN COUNT(*) > 0 THEN CONCAT('Backup table has ', COUNT(*), ' rows - OK to proceed')
        ELSE 'ERROR: No backup found!'
    END AS backup_status
FROM wallets_backup;

-- STEP 2: Drop foreign key constraint from drivers if exists
-- Note: MySQL doesn't support IF EXISTS for DROP FOREIGN KEY
-- Check if constraint exists first
SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'drivers'
    AND CONSTRAINT_NAME = 'fk_driver_wallet'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

-- Drop foreign key only if it exists
SET @sql_drop_fk = IF(@fk_exists > 0,
    'ALTER TABLE drivers DROP FOREIGN KEY fk_driver_wallet',
    'SELECT "Foreign key fk_driver_wallet does not exist, skipping" AS info');
PREPARE stmt FROM @sql_drop_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- STEP 3: Drop wallet_id column from drivers if exists
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'drivers'
    AND COLUMN_NAME = 'wallet_id'
);

-- Drop column only if it exists
SET @sql_drop_col = IF(@column_exists > 0,
    'ALTER TABLE drivers DROP COLUMN wallet_id',
    'SELECT "Column wallet_id does not exist, skipping" AS info');
PREPARE stmt FROM @sql_drop_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- STEP 4: Drop current wallets table
DROP TABLE IF EXISTS wallets;

-- STEP 5: Recreate wallets table with original structure
CREATE TABLE wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    balance DOUBLE NOT NULL DEFAULT 0.0,
    updated_at DATETIME,
    KEY idx_user_id (user_id)
);

-- STEP 6: Restore data from backup
INSERT INTO wallets (id, user_id, balance, updated_at)
SELECT id, user_id, balance, updated_at
FROM wallets_backup;

-- STEP 7: Reset AUTO_INCREMENT to next value
SET @max_id = (SELECT MAX(id) FROM wallets);
SET @sql = CONCAT('ALTER TABLE wallets AUTO_INCREMENT = ', @max_id + 1);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- STEP 8: Verify rollback
SELECT
    'Wallets restored' AS status,
    COUNT(*) AS total_rows,
    MAX(id) AS max_id
FROM wallets;

-- STEP 9: Show sample data
SELECT * FROM wallets LIMIT 5;

SELECT 'Rollback completed! Backup table (wallets_backup) is kept for safety.' AS result;

