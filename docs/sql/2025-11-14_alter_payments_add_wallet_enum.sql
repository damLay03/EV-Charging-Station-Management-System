-- Initialize wallets for all drivers who do not yet have a wallet
-- Run this script once. It will create a wallet row (balance 0) for every driver missing one.
-- Assumes MySQL.

INSERT INTO wallets (user_id, balance, updated_at)
SELECT d.user_id, 0.0, NOW()
FROM drivers d
LEFT JOIN wallets w ON w.user_id = d.user_id
WHERE w.user_id IS NULL;

-- Verification query (optional): list drivers without wallets after insertion (should return 0 rows)
-- SELECT d.user_id FROM drivers d LEFT JOIN wallets w ON w.user_id = d.user_id WHERE w.user_id IS NULL;
