-- Convert payment_method column to VARCHAR to prevent truncation errors when adding new enum values.
-- Run this if your current schema still defines payment_method as ENUM without WALLET.
ALTER TABLE payments MODIFY COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'WALLET';
-- If you already had data with CASH/ZALOPAY they remain intact.

