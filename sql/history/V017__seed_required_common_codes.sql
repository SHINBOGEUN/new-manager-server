-- =============================================================================
-- V017: 서비스 최초 배포용 code_group / common_code 시드
-- =============================================================================
-- 작성일  : 2026-08-19
-- 대상 DB : MariaDB (dcim_new)
--
-- 적용 방법 (예시):
--   mysql -h HOST -P PORT -u dcim -p dcim_new < sql/history/V017__seed_required_common_codes.sql
--
-- 선행 조건: V002 (code_group), V003 (common_code)
--
-- 재실행 안전: 이미 있으면 INSERT 생략
--
-- 참고:
--   LOCATION_TYPE 일부는 V004, MODEL_TYPE/PROTOCOL_TYPE 은 V005,
--   DEVICE_PAGE 는 V014 에서도 넣는다. 빠진 행만 이 스크립트가 채운다.
--   장비 선등록용 location_node 'UNASSIGNED' 는 V004 시드. 이 파일에는 넣지 않음.
--
-- 이 데이터가 있어야 하는 이유:
--   LOCATION_TYPE  — 위치 트리 (UNASSIGNED/CONTAINER/ZONE/ROW/RACK)
--   MODEL_TYPE     — 장비 모델 유형 (PDU/SENSOR/CDU/OTHER)
--   PROTOCOL_TYPE  — 모델 프로토콜·수집 Task scriptType (snmp/modbus/mqtt)
--   DEVICE_PAGE    — 장비 노출 페이지 (ENVIRONMENT/COOLING/ANALYSIS/POWER)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. code_group
-- -----------------------------------------------------------------------------

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

-- -----------------------------------------------------------------------------
-- 2. LOCATION_TYPE
-- -----------------------------------------------------------------------------

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

-- -----------------------------------------------------------------------------
-- 3. MODEL_TYPE
-- -----------------------------------------------------------------------------

INSERT INTO common_code (group_id, code, name, sort_order)
SELECT cg.id, v.code, v.name, v.sort_order
FROM code_group cg
CROSS JOIN (
    SELECT 'PDU'    AS code, 'PDU'    AS name,  1 AS sort_order UNION ALL
    SELECT 'SENSOR' AS code, 'Sensor' AS name,  2 AS sort_order UNION ALL
    SELECT 'CDU'    AS code, 'CDU'    AS name,  3 AS sort_order UNION ALL
    SELECT 'OTHER'  AS code, 'Other'  AS name, 99 AS sort_order
) v
WHERE cg.group_key = 'MODEL_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code cc
      WHERE cc.group_id = cg.id AND cc.code = v.code
  );

-- -----------------------------------------------------------------------------
-- 4. PROTOCOL_TYPE  (수집 Task scriptTypeId 도 이 그룹)
-- -----------------------------------------------------------------------------

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

-- -----------------------------------------------------------------------------
-- 5. DEVICE_PAGE
-- -----------------------------------------------------------------------------

INSERT INTO common_code (group_id, code, name, sort_order)
SELECT cg.id, v.code, v.name, v.sort_order
FROM code_group cg
CROSS JOIN (
    SELECT 'ENVIRONMENT' AS code, 'Environment' AS name, 1 AS sort_order UNION ALL
    SELECT 'COOLING'     AS code, 'Cooling'     AS name, 2 AS sort_order UNION ALL
    SELECT 'ANALYSIS'    AS code, 'Analysis'    AS name, 3 AS sort_order UNION ALL
    SELECT 'POWER'       AS code, 'Power'       AS name, 4 AS sort_order
) v
WHERE cg.group_key = 'DEVICE_PAGE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code cc
      WHERE cc.group_id = cg.id AND cc.code = v.code
  );

-- -----------------------------------------------------------------------------
-- 확인
-- -----------------------------------------------------------------------------
-- SELECT g.group_key, c.id, c.code, c.name, c.sort_order
-- FROM code_group g
-- JOIN common_code c ON c.group_id = g.id
-- WHERE g.group_key IN ('LOCATION_TYPE', 'MODEL_TYPE', 'PROTOCOL_TYPE', 'DEVICE_PAGE')
-- ORDER BY g.group_key, c.sort_order, c.id;
