-- =====================================================
-- Migration: Approve all vehicles
-- Date: 2025-11-22
-- Description: Set approval_status to 'APPROVED' for all vehicles
--              This is useful for testing or migrating old data
-- =====================================================

-- Step 1: Check current status of all vehicles
SELECT
    approval_status,
    COUNT(*) as count
FROM vehicles
GROUP BY approval_status;

-- Step 2: View vehicles that will be updated
SELECT
    vehicle_id,
    license_plate,
    model,
    approval_status,
    submitted_at,
    approved_at
FROM vehicles
WHERE approval_status != 'APPROVED' OR approval_status IS NULL;

-- Step 3: Update all vehicles to APPROVED status
UPDATE vehicles
SET
    approval_status = 'APPROVED',
    approved_at = COALESCE(approved_at, NOW()),  -- Set approved_at if not already set
    submitted_at = COALESCE(submitted_at, NOW()) -- Set submitted_at if not already set
WHERE approval_status != 'APPROVED' OR approval_status IS NULL;

-- Step 4: Verify the update
SELECT
    approval_status,
    COUNT(*) as count
FROM vehicles
GROUP BY approval_status;

-- Step 5: View all approved vehicles
SELECT
    vehicle_id,
    license_plate,
    model,
    approval_status,
    submitted_at,
    approved_at
FROM vehicles
ORDER BY license_plate;

-- =====================================================
-- ALTERNATIVE: Approve ALL vehicles (including already approved)
-- =====================================================
-- Uncomment below if you want to update ALL vehicles regardless of current status
-- UPDATE vehicles
-- SET
--     approval_status = 'APPROVED',
--     approved_at = COALESCE(approved_at, NOW()),
--     submitted_at = COALESCE(submitted_at, NOW());

-- =====================================================
-- ROLLBACK (if needed)
-- =====================================================
-- Reset all vehicles back to PENDING
-- UPDATE vehicles
-- SET
--     approval_status = 'PENDING',
--     approved_at = NULL;

