-- =====================================================
-- SQL Script: Assign Vehicles to Drivers (Simple Random)
-- Generated: November 8, 2025
-- Description: Randomly assign 40 vehicles to 10 drivers (4 each)
-- =====================================================

-- Simple strategy: Give each driver exactly 4 vehicles randomly

SET SQL_SAFE_UPDATES = 0;

-- =====================================================
-- Check what we have
-- =====================================================

SELECT '=== Available Vehicles ===' AS '';
SELECT COUNT(*) AS 'Vehicles without owner' FROM vehicles WHERE owner_id IS NULL;

SELECT '=== Available Drivers ===' AS '';
SELECT COUNT(*) AS 'Total drivers' FROM drivers;

-- =====================================================
-- Random Assignment
-- =====================================================

-- Assign vehicles to driver@gmail.com (4 vehicles)
UPDATE vehicles v
SET v.owner_id = (SELECT user_id FROM users WHERE email = 'driver@gmail.com' LIMIT 1)
WHERE v.vehicle_id IN (
    SELECT vehicle_id FROM (
        SELECT vehicle_id FROM vehicles WHERE owner_id IS NULL ORDER BY RAND() LIMIT 4
    ) AS temp
)
AND v.owner_id IS NULL;

SELECT CONCAT('Assigned 4 vehicles to driver@gmail.com') AS progress;

-- Assign vehicles to driver1@gmail.com (4 vehicles)
UPDATE vehicles v
SET v.owner_id = (SELECT user_id FROM users WHERE email = 'driver1@gmail.com' LIMIT 1)
WHERE v.vehicle_id IN (
    SELECT vehicle_id FROM (
        SELECT vehicle_id FROM vehicles WHERE owner_id IS NULL ORDER BY RAND() LIMIT 4
    ) AS temp
)
AND v.owner_id IS NULL;

SELECT CONCAT('Assigned 4 vehicles to driver1@gmail.com') AS progress;

-- Assign vehicles to driver2@gmail.com (4 vehicles)
UPDATE vehicles v
SET v.owner_id = (SELECT user_id FROM users WHERE email = 'driver2@gmail.com' LIMIT 1)
WHERE v.vehicle_id IN (
    SELECT vehicle_id FROM (
        SELECT vehicle_id FROM vehicles WHERE owner_id IS NULL ORDER BY RAND() LIMIT 4
    ) AS temp
)
AND v.owner_id IS NULL;

SELECT CONCAT('Assigned 4 vehicles to driver2@gmail.com') AS progress;

-- Assign vehicles to driver3@gmail.com (4 vehicles)
UPDATE vehicles v
SET v.owner_id = (SELECT user_id FROM users WHERE email = 'driver3@gmail.com' LIMIT 1)
WHERE v.vehicle_id IN (
    SELECT vehicle_id FROM (
        SELECT vehicle_id FROM vehicles WHERE owner_id IS NULL ORDER BY RAND() LIMIT 4
    ) AS temp
)
AND v.owner_id IS NULL;

SELECT CONCAT('Assigned 4 vehicles to driver3@gmail.com') AS progress;

-- Assign vehicles to driver4@gmail.com (4 vehicles)
UPDATE vehicles v
SET v.owner_id = (SELECT user_id FROM users WHERE email = 'driver4@gmail.com' LIMIT 1)
WHERE v.vehicle_id IN (
    SELECT vehicle_id FROM (
        SELECT vehicle_id FROM vehicles WHERE owner_id IS NULL ORDER BY RAND() LIMIT 4
    ) AS temp
)
AND v.owner_id IS NULL;

SELECT CONCAT('Assigned 4 vehicles to driver4@gmail.com') AS progress;

-- Assign vehicles to driver5@gmail.com (4 vehicles)
UPDATE vehicles v
SET v.owner_id = (SELECT user_id FROM users WHERE email = 'driver5@gmail.com' LIMIT 1)
WHERE v.vehicle_id IN (
    SELECT vehicle_id FROM (
        SELECT vehicle_id FROM vehicles WHERE owner_id IS NULL ORDER BY RAND() LIMIT 4
    ) AS temp
)
AND v.owner_id IS NULL;

SELECT CONCAT('Assigned 4 vehicles to driver5@gmail.com') AS progress;

-- Assign vehicles to driver6@gmail.com (4 vehicles)
UPDATE vehicles v
SET v.owner_id = (SELECT user_id FROM users WHERE email = 'driver6@gmail.com' LIMIT 1)
WHERE v.vehicle_id IN (
    SELECT vehicle_id FROM (
        SELECT vehicle_id FROM vehicles WHERE owner_id IS NULL ORDER BY RAND() LIMIT 4
    ) AS temp
)
AND v.owner_id IS NULL;

SELECT CONCAT('Assigned 4 vehicles to driver6@gmail.com') AS progress;

-- Assign vehicles to driver7@gmail.com (4 vehicles)
UPDATE vehicles v
SET v.owner_id = (SELECT user_id FROM users WHERE email = 'driver7@gmail.com' LIMIT 1)
WHERE v.vehicle_id IN (
    SELECT vehicle_id FROM (
        SELECT vehicle_id FROM vehicles WHERE owner_id IS NULL ORDER BY RAND() LIMIT 4
    ) AS temp
)
AND v.owner_id IS NULL;

SELECT CONCAT('Assigned 4 vehicles to driver7@gmail.com') AS progress;

-- Assign vehicles to driver8@gmail.com (4 vehicles)
UPDATE vehicles v
SET v.owner_id = (SELECT user_id FROM users WHERE email = 'driver8@gmail.com' LIMIT 1)
WHERE v.vehicle_id IN (
    SELECT vehicle_id FROM (
        SELECT vehicle_id FROM vehicles WHERE owner_id IS NULL ORDER BY RAND() LIMIT 4
    ) AS temp
)
AND v.owner_id IS NULL;

SELECT CONCAT('Assigned 4 vehicles to driver8@gmail.com') AS progress;

-- Assign vehicles to driver9@gmail.com (4 vehicles)
UPDATE vehicles v
SET v.owner_id = (SELECT user_id FROM users WHERE email = 'driver9@gmail.com' LIMIT 1)
WHERE v.vehicle_id IN (
    SELECT vehicle_id FROM (
        SELECT vehicle_id FROM vehicles WHERE owner_id IS NULL ORDER BY RAND() LIMIT 4
    ) AS temp
)
AND v.owner_id IS NULL;

SELECT CONCAT('Assigned 4 vehicles to driver9@gmail.com') AS progress;

-- =====================================================
-- Verify Results
-- =====================================================

SELECT '=== FINAL RESULTS ===' AS '';

SELECT
    u.email AS 'Driver',
    COUNT(v.vehicle_id) AS 'Vehicles',
    GROUP_CONCAT(
        CONCAT(v.license_plate, ' - ', v.brand, ' ', v.model)
        ORDER BY v.brand, v.model
        SEPARATOR '\n'
    ) AS 'Vehicle List'
FROM users u
JOIN drivers d ON u.user_id = d.user_id
LEFT JOIN vehicles v ON v.owner_id = d.user_id
WHERE u.role = 'DRIVER'
GROUP BY u.user_id, u.email
ORDER BY u.email;

SELECT '=== STATISTICS ===' AS '';
SELECT
    (SELECT COUNT(*) FROM vehicles WHERE owner_id IS NOT NULL) AS 'Assigned Vehicles',
    (SELECT COUNT(*) FROM vehicles WHERE owner_id IS NULL) AS 'Unassigned Vehicles',
    (SELECT COUNT(DISTINCT owner_id) FROM vehicles WHERE owner_id IS NOT NULL) AS 'Drivers with Vehicles';

-- =====================================================
-- Vehicle Details by Driver
-- =====================================================

SELECT '=== DETAILED VIEW ===' AS '';

SELECT
    u.email AS 'Driver Email',
    CONCAT(u.first_name, ' ', u.last_name) AS 'Driver Name',
    v.license_plate AS 'License Plate',
    CONCAT(v.brand, ' ', v.model) AS 'Vehicle',
    v.battery_capacity_kwh AS 'Battery kWh',
    v.current_soc_percent AS 'SOC %',
    v.battery_type AS 'Battery Type'
FROM users u
JOIN drivers d ON u.user_id = d.user_id
LEFT JOIN vehicles v ON v.owner_id = d.user_id
WHERE u.role = 'DRIVER'
ORDER BY u.email, v.brand, v.model;

SET SQL_SAFE_UPDATES = 1;

-- =====================================================
-- End of Script
-- =====================================================

