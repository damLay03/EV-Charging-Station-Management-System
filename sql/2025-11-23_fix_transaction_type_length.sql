-- Fix: Increase transaction_type column length to accommodate all enum values
-- Date: 2025-11-23
-- Issue: Data truncated for column 'transaction_type' at row 1
-- Cause: BOOKING_DEPOSIT_REFUND has 23 characters but column is VARCHAR(20)

-- Check current length
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    CHARACTER_MAXIMUM_LENGTH
FROM
    INFORMATION_SCHEMA.COLUMNS
WHERE
    TABLE_NAME = 'wallet_transactions'
    AND COLUMN_NAME = 'transaction_type';

-- Alter column to VARCHAR(30) to safely accommodate all enum values
ALTER TABLE wallet_transactions
MODIFY COLUMN transaction_type VARCHAR(30) NOT NULL;

-- Verify the change
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    CHARACTER_MAXIMUM_LENGTH
FROM
    INFORMATION_SCHEMA.COLUMNS
WHERE
    TABLE_NAME = 'wallet_transactions'
    AND COLUMN_NAME = 'transaction_type';

-- List all TransactionType enum values and their lengths:
-- TOPUP_ZALOPAY (13 chars)
-- TOPUP_CASH (10 chars)
-- BOOKING_DEPOSIT (15 chars)
-- BOOKING_DEPOSIT_REFUND (23 chars) ⚠️ LONGEST
-- BOOKING_REFUND (14 chars)
-- CHARGING_PAYMENT (16 chars)
-- ADMIN_ADJUSTMENT (16 chars)
-- PLAN_SUBCRIPTION (16 chars)

