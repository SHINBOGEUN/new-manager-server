-- =============================================================================
-- 단상 PDU (enterprise 6375) 샘플 — 모델/OID/장비/endpoint/Task
-- =============================================================================
-- 작성일  : 2026-08-19
-- 대상 DB : MariaDB (dcim_new)
--
-- 적용 방법:
--   1) V001 ~ V017 적용 (공통코드·collection_task 테이블)
--   2) mysql ... < sql/samples/pdu_1phase_6375_collection_task.sql
--
-- 재실행 안전: 이미 있으면 INSERT 생략
--
-- 원본 walk:
--   snmpwalk -v2c -c public 192.168.14.114 .1.3.6.1.4.1.6375.1
--   1.3.6.1.4.1.6375.1.1.0 = INTEGER: 218
--   1.3.6.1.4.1.6375.1.2.0 = INTEGER: 1
--   1.3.6.1.4.1.6375.1.3.0 = INTEGER: 50
--   1.3.6.1.4.1.6375.1.4.0 = INTEGER: 1
--   1.3.6.1.4.1.6375.1.5.0 = INTEGER: -128
--   1.3.6.1.4.1.6375.1.6.0 = INTEGER: -128
--   1.3.6.1.4.1.6375.1.7.0 = INTEGER: -128
--   1.3.6.1.4.1.6375.1.8.0 = INTEGER: 519
--   1.3.6.1.4.1.6375.1.9.0 = INTEGER: 0
--
-- OID 이름은 MIB가 없어 walk 값으로 추정했다. 나중에 이름/단위만 바꾸면 된다.
-- scalar(.0) 이라 requires_instance = 0. instance 행은 만들지 않는다.
-- community public 은 spec 상수. endpoint 테이블에는 안 넣는다.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 위치
-- -----------------------------------------------------------------------------

INSERT INTO location_node (code, parent_code, location_type_id, name)
SELECT 'PD1ZONE114', NULL, cc.id, '단상 PDU Zone'
FROM common_code cc
INNER JOIN code_group cg ON cg.id = cc.group_id
WHERE cg.group_key = 'LOCATION_TYPE'
  AND cc.code = 'ZONE'
  AND NOT EXISTS (SELECT 1 FROM location_node WHERE code = 'PD1ZONE114');

INSERT INTO location_node (code, parent_code, location_type_id, name)
SELECT 'PD1RACK114', 'PD1ZONE114', cc.id, '단상 PDU Rack'
FROM common_code cc
INNER JOIN code_group cg ON cg.id = cc.group_id
WHERE cg.group_key = 'LOCATION_TYPE'
  AND cc.code = 'RACK'
  AND NOT EXISTS (SELECT 1 FROM location_node WHERE code = 'PD1RACK114');

-- -----------------------------------------------------------------------------
-- 2. 모델 + SNMP 프로토콜
-- -----------------------------------------------------------------------------

INSERT INTO device_model (name, manufacturer, device_type_id, description)
SELECT 'PDU-1P', 'OEM-6375', dt.id, '단상 PDU (OID enterprise 6375)'
FROM common_code dt
INNER JOIN code_group cg ON cg.id = dt.group_id
WHERE cg.group_key = 'MODEL_TYPE'
  AND dt.code = 'PDU'
  AND NOT EXISTS (
      SELECT 1 FROM device_model WHERE name = 'PDU-1P' AND manufacturer = 'OEM-6375'
  );

INSERT INTO device_model_protocol (model_id, protocol_type_id)
SELECT dm.id, pt.id
FROM device_model dm
CROSS JOIN common_code pt
INNER JOIN code_group cg ON cg.id = pt.group_id
WHERE dm.name = 'PDU-1P' AND dm.manufacturer = 'OEM-6375'
  AND cg.group_key = 'PROTOCOL_TYPE' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM device_model_protocol dmp
      WHERE dmp.model_id = dm.id AND dmp.protocol_type_id = pt.id
  );

-- -----------------------------------------------------------------------------
-- 3. 모델 SNMP point (walk 9개, 고정 OID)
-- -----------------------------------------------------------------------------

INSERT INTO device_model_snmp_point (model_protocol_id, name, oid, requires_instance, unit, enabled)
SELECT dmp.id, v.name, v.oid, 0, v.unit, 1
FROM device_model_protocol dmp
INNER JOIN device_model dm ON dm.id = dmp.model_id
INNER JOIN common_code pt ON pt.id = dmp.protocol_type_id
CROSS JOIN (
    SELECT 'V'         AS name, '1.3.6.1.4.1.6375.1.1.0' AS oid, 'V'  AS unit UNION ALL  -- walk 218
    SELECT 'status'    AS name, '1.3.6.1.4.1.6375.1.2.0' AS oid, NULL AS unit UNION ALL  -- walk 1
    SELECT 'Hz'        AS name, '1.3.6.1.4.1.6375.1.3.0' AS oid, 'Hz' AS unit UNION ALL  -- walk 50
    SELECT 'flag'      AS name, '1.3.6.1.4.1.6375.1.4.0' AS oid, NULL AS unit UNION ALL  -- walk 1
    SELECT 'reserved5' AS name, '1.3.6.1.4.1.6375.1.5.0' AS oid, NULL AS unit UNION ALL  -- walk -128
    SELECT 'reserved6' AS name, '1.3.6.1.4.1.6375.1.6.0' AS oid, NULL AS unit UNION ALL  -- walk -128
    SELECT 'reserved7' AS name, '1.3.6.1.4.1.6375.1.7.0' AS oid, NULL AS unit UNION ALL  -- walk -128
    SELECT 'W'         AS name, '1.3.6.1.4.1.6375.1.8.0' AS oid, 'W'  AS unit UNION ALL  -- walk 519
    SELECT 'reserved9' AS name, '1.3.6.1.4.1.6375.1.9.0' AS oid, NULL AS unit             -- walk 0
) v
WHERE dm.name = 'PDU-1P' AND dm.manufacturer = 'OEM-6375' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM device_model_snmp_point p
      WHERE p.model_protocol_id = dmp.id AND p.name = v.name
  );

-- -----------------------------------------------------------------------------
-- 4. 장비
-- -----------------------------------------------------------------------------

INSERT INTO devices (model_id, location_node_code, name, description, enabled)
SELECT dm.id, 'PD1RACK114', 'PDU-1P-114', '192.168.14.114 단상 PDU', 1
FROM device_model dm
WHERE dm.name = 'PDU-1P' AND dm.manufacturer = 'OEM-6375'
  AND NOT EXISTS (
      SELECT 1 FROM devices d
      WHERE d.location_node_code = 'PD1RACK114' AND d.name = 'PDU-1P-114'
  );

INSERT INTO device_page (device_id, page_code_id)
SELECT d.id, cc.id
FROM devices d
CROSS JOIN common_code cc
INNER JOIN code_group cg ON cg.id = cc.group_id
WHERE d.location_node_code = 'PD1RACK114' AND d.name = 'PDU-1P-114'
  AND cg.group_key = 'DEVICE_PAGE' AND cc.code = 'POWER'
  AND NOT EXISTS (
      SELECT 1 FROM device_page dp
      WHERE dp.device_id = d.id AND dp.page_code_id = cc.id
  );

-- -----------------------------------------------------------------------------
-- 5. SNMP endpoint (community=public 은 spec 쪽 상수)
-- -----------------------------------------------------------------------------

INSERT INTO device_protocol_endpoint (device_id, protocol_type_id, host, port, enabled)
SELECT d.id, pt.id, '192.168.14.114', 161, 1
FROM devices d
CROSS JOIN common_code pt
INNER JOIN code_group cg ON cg.id = pt.group_id
WHERE d.location_node_code = 'PD1RACK114' AND d.name = 'PDU-1P-114'
  AND cg.group_key = 'PROTOCOL_TYPE' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM device_protocol_endpoint e
      WHERE e.device_id = d.id AND e.protocol_type_id = pt.id
  );

-- -----------------------------------------------------------------------------
-- 6. 수집 Task + 1분 그룹 + 장비 매핑
-- -----------------------------------------------------------------------------

INSERT INTO collection_task (name, model_id, script_type_id, active)
SELECT 'PDU-1P SNMP 수집', dm.id, pt.id, 1
FROM device_model dm
CROSS JOIN common_code pt
INNER JOIN code_group cg ON cg.id = pt.group_id
WHERE dm.name = 'PDU-1P' AND dm.manufacturer = 'OEM-6375'
  AND cg.group_key = 'PROTOCOL_TYPE' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM collection_task t
      WHERE t.model_id = dm.id AND t.script_type_id = pt.id
  );

INSERT INTO collection_task_group (task_id, name, cron_expression, active)
SELECT t.id, '기본 그룹', '0 */1 * * * *', 1
FROM collection_task t
INNER JOIN device_model dm ON dm.id = t.model_id
INNER JOIN common_code pt ON pt.id = t.script_type_id
INNER JOIN code_group cg ON cg.id = pt.group_id
WHERE dm.name = 'PDU-1P' AND dm.manufacturer = 'OEM-6375'
  AND cg.group_key = 'PROTOCOL_TYPE' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM collection_task_group g WHERE g.task_id = t.id
  );

INSERT INTO collection_task_device (group_id, device_id)
SELECT g.id, d.id
FROM collection_task_group g
INNER JOIN collection_task t ON t.id = g.task_id
INNER JOIN device_model dm ON dm.id = t.model_id
INNER JOIN devices d ON d.model_id = dm.id AND d.name = 'PDU-1P-114'
WHERE dm.name = 'PDU-1P' AND dm.manufacturer = 'OEM-6375'
  AND NOT EXISTS (
      SELECT 1 FROM collection_task_device m
      WHERE m.group_id = g.id AND m.device_id = d.id
  );

-- -----------------------------------------------------------------------------
-- 7. generated_spec (앱 재생성과 같은 JSON. API GET 후에도 동일해야 함)
-- -----------------------------------------------------------------------------

UPDATE collection_task_group g
INNER JOIN collection_task t ON t.id = g.task_id
INNER JOIN device_model dm ON dm.id = t.model_id
INNER JOIN devices d ON d.model_id = dm.id AND d.name = 'PDU-1P-114'
INNER JOIN device_protocol_endpoint e ON e.device_id = d.id
INNER JOIN common_code pt ON pt.id = e.protocol_type_id
INNER JOIN code_group cg ON cg.id = pt.group_id AND cg.group_key = 'PROTOCOL_TYPE' AND pt.code = 'snmp'
SET g.generated_spec = CONCAT(
    '{',
    '"taskId":', t.id, ',',
    '"groupId":', g.id, ',',
    '"modelId":', dm.id, ',',
    '"protocol":"snmp",',
    '"cronExpression":"', g.cron_expression, '",',
    '"community":"public",',
    '"timeoutMs":2000,',
    '"retries":1,',
    '"maxConcurrency":10,',
    '"oids":[',
      '{"name":"V","template":"1.3.6.1.4.1.6375.1.1.0","requiresInstance":false},',
      '{"name":"status","template":"1.3.6.1.4.1.6375.1.2.0","requiresInstance":false},',
      '{"name":"Hz","template":"1.3.6.1.4.1.6375.1.3.0","requiresInstance":false},',
      '{"name":"flag","template":"1.3.6.1.4.1.6375.1.4.0","requiresInstance":false},',
      '{"name":"reserved5","template":"1.3.6.1.4.1.6375.1.5.0","requiresInstance":false},',
      '{"name":"reserved6","template":"1.3.6.1.4.1.6375.1.6.0","requiresInstance":false},',
      '{"name":"reserved7","template":"1.3.6.1.4.1.6375.1.7.0","requiresInstance":false},',
      '{"name":"W","template":"1.3.6.1.4.1.6375.1.8.0","requiresInstance":false},',
      '{"name":"reserved9","template":"1.3.6.1.4.1.6375.1.9.0","requiresInstance":false}',
    '],',
    '"targets":[{',
      '"deviceId":', d.id, ',',
      '"host":"', e.host, '",',
      '"port":', e.port, ',',
      '"instanceId":null',
    '}],',
    '"skipped":[]',
    '}'
)
WHERE dm.name = 'PDU-1P' AND dm.manufacturer = 'OEM-6375';

-- -----------------------------------------------------------------------------
-- 확인
-- -----------------------------------------------------------------------------
-- SELECT t.id AS task_id, t.name, g.id AS group_id, g.cron_expression, d.id AS device_id, e.host, e.port
-- FROM collection_task t
-- JOIN collection_task_group g ON g.task_id = t.id
-- JOIN collection_task_device m ON m.group_id = g.id
-- JOIN devices d ON d.id = m.device_id
-- JOIN device_protocol_endpoint e ON e.device_id = d.id
-- JOIN device_model dm ON dm.id = t.model_id
-- WHERE dm.name = 'PDU-1P';
--
-- GET /api/manager/collector/tasks?modelId={task.model_id}
-- =============================================================================
