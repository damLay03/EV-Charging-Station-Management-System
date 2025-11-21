-- Renames every charging point per station to TS-1, TS-2, ... using a deterministic ordering.
-- Compatible with MySQL 8+ (requires window functions). Run inside maintenance window.

START TRANSACTION;

WITH ranked_points AS (
    SELECT
        cp.point_id,
        CONCAT('TS-', LPAD(ROW_NUMBER() OVER (
            PARTITION BY cp.station_id
            ORDER BY cp.point_id
        ), 1, '')) AS new_name
    FROM charging_points cp
    WHERE cp.station_id IS NOT NULL
)
UPDATE charging_points AS cp
INNER JOIN ranked_points AS rp ON rp.point_id = cp.point_id
SET cp.name = rp.new_name
WHERE cp.station_id IS NOT NULL;

COMMIT;

-- Rollback helper (snapshot names before running!)
-- UPDATE charging_points SET name = NULL WHERE name LIKE 'TS-%';
