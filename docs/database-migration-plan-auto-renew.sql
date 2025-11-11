-- Migration script: Add plan_auto_renew column to drivers table
-- Purpose: Support cancel/reactivate auto-renewal feature
-- Date: 2025-11-11

-- Add plan_auto_renew column
ALTER TABLE drivers
ADD COLUMN plan_auto_renew BOOLEAN DEFAULT TRUE COMMENT 'Trạng thái tự động gia hạn gói (true = bật, false = tắt)';

-- Update existing records to have auto-renewal enabled by default
UPDATE drivers
SET plan_auto_renew = TRUE
WHERE plan_auto_renew IS NULL;

-- Verify the changes
SELECT user_id, plan_id, plan_subscription_date, plan_auto_renew
FROM drivers
LIMIT 10;

