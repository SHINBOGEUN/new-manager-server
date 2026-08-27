-- =============================================================================
-- REQUIRED_BOOTSTRAP: 신규 배포 시 DB에 꼭 있어야 하는 데이터
-- =============================================================================
-- 작성일  : 2026-08-26
-- 대상 DB : MariaDB (dcim_new)
--
-- 적용 시점:
--   1) sql/history/ V001~V019 스키마 적용 후
--   2) 이 파일 1회 (재실행 안전 — 이미 있으면 생략)
--
-- 적용 방법 (예시):
--   mysql -h HOST -P PORT -u dcim -p dcim_new < sql/seed/REQUIRED_BOOTSTRAP.sql
--
-- 포함 (시스템 필수 — 현장 IP/장비/위젯 없음):
--   code_group / common_code
--     LOCATION_TYPE  — 위치 트리 타입
--     LOCATION_PATH  — 차트 Path 그룹 (A/B/C)
--     MODEL_TYPE     — 장비 모델 유형 (device_model.device_type_id)
--     PROTOCOL_TYPE  — 프로토콜·수집 scriptType (snmp/modbus/mqtt)
--     DEVICE_PAGE    — UI 페이지 코드 (위젯 page_code_id)
--   location_node
--     UNASSIGNED     — 장비 위치 미지정 시 필수 FK 대상
--
-- 코드 표기:
--   LOCATION / MODEL / DEVICE_PAGE 는 UPPER_SNAKE (예: ZONE, PDU, ENVIRONMENT)
--   PROTOCOL 은 소문자 (snmp/modbus/mqtt) — 앱·수집 스펙과 동일
--   기존 DB에 소문자(zone/pdu/…)가 있어도 utf8mb4_unicode_ci 로 중복 INSERT 안 됨
--
-- 포함하지 않음 (현장 데이터):
--   users, device_model, devices, endpoint, collection_task, page_widget …
--   → UI /ops-console.html 로 등록. 백업은 sql/dumps/
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. code_group
-- ---------------------------------------------------------------------------

INSERT INTO code_group (group_key, group_name)
SELECT 'LOCATION_TYPE', 'Location Type'
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE group_key = 'LOCATION_TYPE');

INSERT INTO code_group (group_key, group_name)
SELECT 'MODEL_TYPE', 'Model Type'
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE group_key = 'MODEL_TYPE');

INSERT INTO code_group (group_key, group_name)
SELECT 'PROTOCOL_TYPE', 'Protocol Type'
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE group_key = 'PROTOCOL_TYPE');

INSERT INTO code_group (group_key, group_name)
SELECT 'DEVICE_PAGE', 'Device Page'
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE group_key = 'DEVICE_PAGE');

INSERT INTO code_group (group_key, group_name)
SELECT 'LOCATION_PATH', 'Location Path'
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE group_key = 'LOCATION_PATH');

-- ---------------------------------------------------------------------------
-- 2. LOCATION_TYPE
-- ---------------------------------------------------------------------------

INSERT INTO common_code (group_id, code, name, sort_order)
SELECT cg.id, v.code, v.name, v.sort_order
FROM code_group cg
CROSS JOIN (
    SELECT 'UNASSIGNED' AS code, '미배정'   AS name, -1 AS sort_order UNION ALL
    SELECT 'CONTAINER'  AS code, '컨테이너' AS name,  0 AS sort_order UNION ALL
    SELECT 'ZONE'       AS code, '존'       AS name,  1 AS sort_order UNION ALL
    SELECT 'ROW'        AS code, '열'       AS name,  2 AS sort_order UNION ALL
    SELECT 'RACK'       AS code, '랙'       AS name,  3 AS sort_order
) v
WHERE cg.group_key = 'LOCATION_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code cc
      WHERE cc.group_id = cg.id AND cc.code = v.code
  );

-- ---------------------------------------------------------------------------
-- 3. MODEL_TYPE  (앱 상수: MODEL_TYPE — 실제 DB·쇼룸에서 쓰는 유형 반영)
-- ---------------------------------------------------------------------------

INSERT INTO common_code (group_id, code, name, sort_order)
SELECT cg.id, v.code, v.name, v.sort_order
FROM code_group cg
CROSS JOIN (
    SELECT 'PDU'                AS code, 'PDU'                AS name,  1 AS sort_order UNION ALL
    SELECT 'UPS'                AS code, 'UPS'                AS name,  2 AS sort_order UNION ALL
    SELECT 'SENSOR'             AS code, 'Sensor'             AS name,  3 AS sort_order UNION ALL
    SELECT 'CDU'                AS code, 'CDU'                AS name,  4 AS sort_order UNION ALL
    SELECT 'RDC'                AS code, 'RDC'                AS name,  5 AS sort_order UNION ALL
    SELECT 'DISTRIBUTION_BOARD' AS code, 'Distribution Board' AS name,  6 AS sort_order UNION ALL
    SELECT 'OTHER'              AS code, 'Other'              AS name, 99 AS sort_order
) v
WHERE cg.group_key = 'MODEL_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code cc
      WHERE cc.group_id = cg.id AND cc.code = v.code
  );

-- ---------------------------------------------------------------------------
-- 4. PROTOCOL_TYPE  (수집 Task scriptType 도 동일 그룹)
-- ---------------------------------------------------------------------------

INSERT INTO common_code (group_id, code, name, sort_order)
SELECT cg.id, v.code, v.name, v.sort_order
FROM code_group cg
CROSS JOIN (
    SELECT 'snmp'   AS code, 'SNMP'   AS name, 1 AS sort_order UNION ALL
    SELECT 'modbus' AS code, 'Modbus' AS name, 2 AS sort_order UNION ALL
    SELECT 'mqtt'   AS code, 'MQTT'   AS name, 3 AS sort_order
) v
WHERE cg.group_key = 'PROTOCOL_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code cc
      WHERE cc.group_id = cg.id AND cc.code = v.code
  );

-- ---------------------------------------------------------------------------
-- 5. DEVICE_PAGE  (페이지 위젯 page_code)
-- ---------------------------------------------------------------------------

INSERT INTO common_code (group_id, code, name, sort_order)
SELECT cg.id, v.code, v.name, v.sort_order
FROM code_group cg
CROSS JOIN (
    SELECT 'ENVIRONMENT' AS code, 'Environment' AS name, 1 AS sort_order UNION ALL
    SELECT 'COOLING'     AS code, 'Cooling'     AS name, 2 AS sort_order UNION ALL
    SELECT 'ANALYSIS'    AS code, 'Analysis'    AS name, 3 AS sort_order UNION ALL
    SELECT 'POWER'       AS code, 'Power'       AS name, 4 AS sort_order UNION ALL
    SELECT 'dashboard'   AS code, 'Dashboard'   AS name, 5 AS sort_order
) v
WHERE cg.group_key = 'DEVICE_PAGE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code cc
      WHERE cc.group_id = cg.id AND cc.code = v.code
  );

-- ---------------------------------------------------------------------------
-- 5b. LOCATION_PATH  (PDU Path 피드 / 차트 by_path — devices.path_code_id)
-- ---------------------------------------------------------------------------

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
-- 6. location_node UNASSIGNED  (devices.location_node_code NOT NULL)
--    code='UNASSIGNED' 고정. 삭제·이름 변경 금지.
-- ---------------------------------------------------------------------------

INSERT INTO location_node (code, parent_code, location_type_id, name)
SELECT 'UNASSIGNED', NULL, cc.id, '미배정'
FROM common_code cc
INNER JOIN code_group cg ON cg.id = cc.group_id
WHERE cg.group_key = 'LOCATION_TYPE'
  AND cc.code = 'UNASSIGNED'
  AND NOT EXISTS (
      SELECT 1 FROM location_node WHERE code = 'UNASSIGNED'
  );

-- ---------------------------------------------------------------------------
-- 확인 (수동)
-- ---------------------------------------------------------------------------
-- SELECT g.group_key, c.code, c.name, c.sort_order
-- FROM code_group g
-- JOIN common_code c ON c.group_id = g.id
-- WHERE g.group_key IN ('LOCATION_TYPE', 'LOCATION_PATH', 'MODEL_TYPE', 'PROTOCOL_TYPE', 'DEVICE_PAGE')
-- ORDER BY g.group_key, c.sort_order, c.id;
--
-- SELECT * FROM location_node WHERE code = 'UNASSIGNED';
