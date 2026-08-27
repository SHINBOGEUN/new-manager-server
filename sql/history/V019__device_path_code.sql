-- =============================================================================
-- V019: devices.path_code_id + LOCATION_PATH 공통코드
-- =============================================================================
-- 작성일  : 2026-08-27
-- 대상 DB : MariaDB (dcim_new)
--
-- 목적:
--   Path = PDU(장비)별 전원 피드 (A/B/C… N개, nullable). Rack은 위치만.
--   차트 seriesMode=by_path 는 device.path_code_id 기준으로 합산.
--
-- 적용:
--   mysql -h HOST -P PORT -u dcim -p dcim_new < sql/history/V019__device_path_code.sql
--
-- 선행: V003 (common_code), V007 (devices)
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1) LOCATION_PATH code_group + A/B/C 시드
-- ---------------------------------------------------------------------------
INSERT INTO code_group (group_key, group_name)
SELECT 'LOCATION_PATH', 'Location Path'
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE group_key = 'LOCATION_PATH');

INSERT INTO common_code (group_id, code, name, sort_order)
SELECT cg.id, v.code, v.name, v.sort_order
FROM code_group cg
CROSS JOIN (
    SELECT 'A' AS code, 'A Path' AS name, 1 AS sort_order UNION ALL
    SELECT 'B' AS code, 'B Path' AS name, 2 AS sort_order UNION ALL
    SELECT 'C' AS code, 'C Path' AS name, 3 AS sort_order
) v
WHERE cg.group_key = 'LOCATION_PATH'
  AND NOT EXISTS (
      SELECT 1 FROM common_code cc
      WHERE cc.group_id = cg.id AND cc.code = v.code
  );

-- ---------------------------------------------------------------------------
-- 2) devices.path_code_id
-- ---------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'devices'
      AND COLUMN_NAME = 'path_code_id'
);

SET @sql := IF(
    @col_exists = 0,
    'ALTER TABLE devices
        ADD COLUMN path_code_id INT NULL COMMENT ''LOCATION_PATH common_code.id (PDU Path 피드)'' AFTER location_node_code,
        ADD KEY idx_devices_path_code_id (path_code_id),
        ADD CONSTRAINT fk_devices_path_code_id
            FOREIGN KEY (path_code_id) REFERENCES common_code (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE',
    'SELECT ''devices.path_code_id already exists'' AS info'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
