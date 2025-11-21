-- Migration script: Change wallet ID from BIGINT auto-increment to VARCHAR UUID
-- Date: 2025-11-21
-- Purpose: Convert wallet primary key to UUID for better scalability
-- IMPORTANT: Run this on a TEST database first!

-- STEP 0: Check current state
SELECT 'Current wallets table structure' AS info;
DESCRIBE wallets;

-- STEP 1: Backup existing wallets (REQUIRED!)
DROP TABLE IF EXISTS wallets_backup;
CREATE TABLE wallets_backup AS SELECT * FROM wallets;
SELECT CONCAT('Backed up ', COUNT(*), ' wallets') AS result FROM wallets_backup;

-- STEP 2: Add new UUID column temporarily
ALTER TABLE wallets ADD COLUMN wallet_id VARCHAR(36) NULL AFTER id;

-- STEP 3: Generate UUIDs for existing wallets
UPDATE wallets SET wallet_id = UUID() WHERE wallet_id IS NULL;

-- STEP 4: Remove AUTO_INCREMENT from id column first
-- This is required before we can drop the primary key
ALTER TABLE wallets MODIFY COLUMN id BIGINT NOT NULL;

-- STEP 5: Drop the old primary key (now possible since AUTO_INCREMENT is removed)
ALTER TABLE wallets DROP PRIMARY KEY;

-- STEP 6: Drop the old id column
ALTER TABLE wallets DROP COLUMN id;

-- STEP 7: Set wallet_id as NOT NULL and primary key
ALTER TABLE wallets MODIFY COLUMN wallet_id VARCHAR(36) NOT NULL;
ALTER TABLE wallets ADD PRIMARY KEY (wallet_id);

-- STEP 8: Add wallet_id column to drivers table (foreign key reference)
ALTER TABLE drivers ADD COLUMN wallet_id VARCHAR(36) NULL AFTER plan_id;

-- STEP 9: Link existing drivers to their wallets
UPDATE drivers d
INNER JOIN wallets w ON d.user_id = w.user_id
SET d.wallet_id = w.wallet_id;

-- STEP 10: Add foreign key constraint
ALTER TABLE drivers
ADD CONSTRAINT fk_driver_wallet
FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id) ON DELETE SET NULL;

-- STEP 11: Add index for better performance
CREATE INDEX idx_drivers_wallet_id ON drivers(wallet_id);

-- STEP 12: Verify migration results
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

SELECT 'Migration completed successfully!' AS result;

