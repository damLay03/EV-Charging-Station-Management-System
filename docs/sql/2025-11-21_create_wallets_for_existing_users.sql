-- Script to create wallets for existing users who don't have one yet
-- Date: 2025-11-21
-- Purpose: Backfill wallets for users created before wallet auto-creation feature
-- Note: This is a SIMPLIFIED version that only creates wallets in the wallets table

-- Create wallets for all drivers who don't have a wallet yet
-- Note: wallet_id will be auto-generated as UUID
INSERT INTO wallets (wallet_id, user_id, balance, updated_at)
SELECT
    UUID() AS wallet_id,
    d.user_id,
    0.0 AS balance,
    NOW() AS updated_at
FROM drivers d
WHERE NOT EXISTS (
    SELECT 1 FROM wallets w WHERE w.user_id = d.user_id
);

-- Verify the results
SELECT
    COUNT(*) AS total_drivers,
    (SELECT COUNT(*) FROM wallets WHERE user_id IN (SELECT user_id FROM drivers)) AS drivers_with_wallet,
    COUNT(*) - (SELECT COUNT(*) FROM wallets WHERE user_id IN (SELECT user_id FROM drivers)) AS drivers_without_wallet
FROM drivers;

-- Show users who now have wallets (linked via user_id)
SELECT
    u.user_id,
    u.email,
    u.first_name,
    u.last_name,
    w.wallet_id,
    w.balance,
    w.updated_at
FROM users u
INNER JOIN drivers d ON u.user_id = d.user_id
INNER JOIN wallets w ON d.user_id = w.user_id
ORDER BY w.updated_at DESC;


