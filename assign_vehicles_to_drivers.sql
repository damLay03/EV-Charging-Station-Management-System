-- =====================================================
-- SQL Script: Assign Vehicles to Drivers Randomly
-- Generated: November 8, 2025
-- Description: Randomly assign vehicles to drivers
-- =====================================================

-- Each driver can have multiple vehicles
-- We'll assign 1-3 vehicles per driver randomly

-- =====================================================
-- Safety Settings
-- =====================================================

SET SQL_SAFE_UPDATES = 0;

-- =====================================================
-- BEFORE: Check current status
-- =====================================================

SELECT '=== DRIVERS WITHOUT VEHICLES ===' AS '';
SELECT
    d.user_id,
    u.email,
    CONCAT(u.first_name, ' ', u.last_name) AS driver_name,
    (SELECT COUNT(*) FROM vehicles v WHERE v.owner_id = d.user_id) AS vehicle_count
FROM drivers d
JOIN users u ON d.user_id = u.user_id
WHERE u.role = 'DRIVER'
ORDER BY u.email;

SELECT '=== VEHICLES WITHOUT OWNER ===' AS '';
SELECT
    vehicle_id,
    license_plate,
    model,
    brand,
    battery_capacity_kwh,
    owner_id
FROM vehicles
WHERE owner_id IS NULL
ORDER BY brand, model;

SELECT '=== SUMMARY BEFORE ===' AS '';
SELECT
    (SELECT COUNT(*) FROM drivers) AS 'Total Drivers',
    (SELECT COUNT(*) FROM vehicles WHERE owner_id IS NULL) AS 'Unassigned Vehicles',
    (SELECT COUNT(*) FROM vehicles) AS 'Total Vehicles';

-- =====================================================
-- ASSIGNMENT: Randomly assign vehicles to drivers
-- =====================================================

-- Strategy: Assign 1-4 vehicles per driver randomly
-- Total: 10 drivers, 40 vehicles = 4 vehicles per driver on average

-- Create a temporary mapping table with random assignment
CREATE TEMPORARY TABLE IF NOT EXISTS temp_vehicle_driver_map AS
SELECT
    v.vehicle_id,
    v.license_plate,
    v.brand,
    v.model,
    d.user_id AS driver_user_id,
    u.email AS driver_email,
    ROW_NUMBER() OVER (PARTITION BY d.user_id ORDER BY RAND()) AS vehicle_num
FROM vehicles v
CROSS JOIN (
    SELECT
        d2.user_id,
        u2.email,
        ROW_NUMBER() OVER (ORDER BY RAND()) AS driver_order
    FROM drivers d2
    JOIN users u2 ON d2.user_id = u2.user_id
    WHERE u2.role = 'DRIVER'
) AS d
JOIN users u ON d.user_id = u.user_id
WHERE v.owner_id IS NULL
AND (
    -- Assign vehicles cyclically to drivers
    (SELECT @row_num := @row_num + 1) % (SELECT COUNT(*) FROM drivers) = d.driver_order - 1
    OR 1=1  -- Allow all combinations, then filter by row number
);

-- Reset row counter
SET @row_num = 0;

-- Create better mapping: Distribute 40 vehicles among 10 drivers (4 each on average)
DROP TEMPORARY TABLE IF EXISTS temp_vehicle_driver_map;

CREATE TEMPORARY TABLE temp_vehicle_driver_map AS
SELECT
    v.vehicle_id,
    v.license_plate,
    v.brand,
    v.model,
    driver_list.user_id AS driver_user_id,
    driver_list.email AS driver_email
FROM (
    SELECT
        vehicle_id,
        license_plate,
        brand,
        model,
        ROW_NUMBER() OVER (ORDER BY RAND()) AS vehicle_row
    FROM vehicles
    WHERE owner_id IS NULL
) AS v
JOIN (
    SELECT
        d.user_id,
        u.email,
        ROW_NUMBER() OVER (ORDER BY u.email) AS driver_row
    FROM drivers d
    JOIN users u ON d.user_id = u.user_id
    WHERE u.role = 'DRIVER'
) AS driver_list
-- Distribute vehicles: driver_row = ((vehicle_row - 1) % total_drivers) + 1
ON driver_list.driver_row = ((v.vehicle_row - 1) % 10) + 1;

-- Show assignment plan
SELECT '=== ASSIGNMENT PLAN ===' AS '';
SELECT
    driver_email AS 'Driver Email',
    COUNT(*) AS 'Vehicles Assigned',
    GROUP_CONCAT(CONCAT(brand, ' ', model) SEPARATOR ', ') AS 'Vehicle Models'
FROM temp_vehicle_driver_map
GROUP BY driver_user_id, driver_email
ORDER BY driver_email;

-- Perform the assignment
UPDATE vehicles v
INNER JOIN temp_vehicle_driver_map m ON v.vehicle_id = m.vehicle_id
SET v.owner_id = m.driver_user_id
WHERE v.vehicle_id = m.vehicle_id;

-- Clean up temporary table
DROP TEMPORARY TABLE IF EXISTS temp_vehicle_driver_map;

-- =====================================================
-- AFTER: Verify results
-- =====================================================

SELECT '=== DRIVERS WITH VEHICLES ===' AS '';
SELECT
    u.email AS 'Driver Email',
    CONCAT(u.first_name, ' ', u.last_name) AS 'Driver Name',
    COUNT(v.vehicle_id) AS 'Number of Vehicles',
    GROUP_CONCAT(CONCAT(v.license_plate, ' (', v.brand, ' ', v.model, ')') SEPARATOR ', ') AS 'Vehicles'
FROM drivers d
JOIN users u ON d.user_id = u.user_id
LEFT JOIN vehicles v ON v.owner_id = d.user_id
WHERE u.role = 'DRIVER'
GROUP BY d.user_id, u.email, u.first_name, u.last_name
ORDER BY u.email;

SELECT '=== VEHICLES BY OWNER ===' AS '';
SELECT
    v.license_plate AS 'License Plate',
    CONCAT(v.brand, ' ', v.model) AS 'Vehicle',
    v.battery_capacity_kwh AS 'Battery (kWh)',
    v.current_soc_percent AS 'Battery %',
    u.email AS 'Owner Email',
    CONCAT(u.first_name, ' ', u.last_name) AS 'Owner Name'
FROM vehicles v
LEFT JOIN users u ON v.owner_id = u.user_id
ORDER BY v.brand, v.model, v.license_plate;

SELECT '=== SUMMARY AFTER ===' AS '';
SELECT
    (SELECT COUNT(*) FROM drivers) AS 'Total Drivers',
    (SELECT COUNT(DISTINCT owner_id) FROM vehicles WHERE owner_id IS NOT NULL) AS 'Drivers with Vehicles',
    (SELECT COUNT(*) FROM vehicles WHERE owner_id IS NOT NULL) AS 'Assigned Vehicles',
    (SELECT COUNT(*) FROM vehicles WHERE owner_id IS NULL) AS 'Unassigned Vehicles',
    (SELECT COUNT(*) FROM vehicles) AS 'Total Vehicles',
    ROUND((SELECT COUNT(*) FROM vehicles WHERE owner_id IS NOT NULL) / (SELECT COUNT(DISTINCT owner_id) FROM vehicles WHERE owner_id IS NOT NULL), 1) AS 'Avg Vehicles per Driver';

-- Distribution statistics
SELECT '=== DISTRIBUTION STATISTICS ===' AS '';
SELECT
    vehicle_count AS 'Number of Vehicles',
    COUNT(*) AS 'Number of Drivers'
FROM (
    SELECT
        d.user_id,
        COUNT(v.vehicle_id) AS vehicle_count
    FROM drivers d
    LEFT JOIN vehicles v ON v.owner_id = d.user_id
    GROUP BY d.user_id
) AS driver_stats
GROUP BY vehicle_count
ORDER BY vehicle_count;

-- =====================================================
-- UNDO: Clear all vehicle assignments (if needed)
-- =====================================================

-- Uncomment to clear all vehicle assignments:
/*
SET SQL_SAFE_UPDATES = 0;
UPDATE vehicles SET owner_id = NULL WHERE owner_id IS NOT NULL;
SELECT 'All vehicle assignments cleared!' AS message;
SET SQL_SAFE_UPDATES = 1;
*/

-- =====================================================
-- Re-enable safe update mode
-- =====================================================

SET SQL_SAFE_UPDATES = 1;

-- =====================================================
-- End of Script
-- =====================================================

-- SUMMARY:
-- This script randomly assigns 40 vehicles to 10 drivers
-- Distribution: ~4 vehicles per driver (evenly distributed)
-- Each driver will own 4 vehicles (balanced distribution)
-- Assignment is randomized but balanced

