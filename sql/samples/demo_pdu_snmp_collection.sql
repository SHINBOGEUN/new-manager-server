-- =============================================================================
-- demo_pdu_snmp_collection: 단상 PDU SNMP 수집 샘플 (모델/OID/장비/Task)
-- =============================================================================
-- 작성일  : 2026-08-26 (6375 + Raritan 샘플 통합)
-- 대상 DB : MariaDB (dcim_new)
--
-- 적용 방법:
--   1) history + seed/REQUIRED_BOOTSTRAP 적용
--   2) mysql ... < sql/samples/demo_pdu_snmp_collection.sql
--
-- 재실행 안전: 이미 있으면 INSERT 생략
--
-- 포함:
--   A) OEM-6375 단상 PDU 1대 (192.168.14.114:161)
--   B) Raritan 단상 PDU 4대 (14.42.43.207:30263~30266)
-- =============================================================================

-- ##### A) OEM enterprise 6375 #####

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

-- ##### B) Raritan enterprise 13742 #####

--
-- 적용 방법:
--   1) V001 ~ V017 적용 (공통코드·collection_task 테이블)
--
-- 재실행 안전: 이미 있으면 INSERT 생략
--
-- 장비 (SNMP proxy port — host 동일, port 상이):
--   14.42.43.207:30263  phase-1
--   14.42.43.207:30264  phase-1
--   14.42.43.207:30265  phase-1
--   14.42.43.207:30266  phase-1
--
-- OID (Raritan rack PDU, outlet/bank 1 고정 경로):
--   AMP       1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.1
--   PF        1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.7
--   TOTAL_WT  1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.5
--   TOTAL_KWH 1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.8
--
-- scalar 고정 OID → requires_instance = 0. instance 행 없음.
-- community public 은 spec 상수. endpoint 테이블에는 안 넣는다.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 위치

-- ##### B) Raritan enterprise 13742 #####

-- -----------------------------------------------------------------------------

INSERT INTO location_node (code, parent_code, location_type_id, name)
SELECT 'PD1ZONE207', NULL, cc.id, 'Raritan PDU Zone'
FROM common_code cc
INNER JOIN code_group cg ON cg.id = cc.group_id
WHERE cg.group_key = 'LOCATION_TYPE'
  AND cc.code = 'ZONE'
  AND NOT EXISTS (SELECT 1 FROM location_node WHERE code = 'PD1ZONE207');

INSERT INTO location_node (code, parent_code, location_type_id, name)
SELECT 'PD1RACK207', 'PD1ZONE207', cc.id, 'Raritan PDU Rack'
FROM common_code cc
INNER JOIN code_group cg ON cg.id = cc.group_id
WHERE cg.group_key = 'LOCATION_TYPE'
  AND cc.code = 'RACK'
  AND NOT EXISTS (SELECT 1 FROM location_node WHERE code = 'PD1RACK207');

-- -----------------------------------------------------------------------------
-- 2. 모델 + SNMP 프로토콜
-- -----------------------------------------------------------------------------

INSERT INTO device_model (name, manufacturer, device_type_id, description)
SELECT 'PDU-1P', 'Raritan', dt.id, '단상 PDU phase-1 (OID enterprise 13742)'
FROM common_code dt
INNER JOIN code_group cg ON cg.id = dt.group_id
WHERE cg.group_key = 'MODEL_TYPE'
  AND dt.code = 'PDU'
  AND NOT EXISTS (
      SELECT 1 FROM device_model WHERE name = 'PDU-1P' AND manufacturer = 'Raritan'
  );

INSERT INTO device_model_protocol (model_id, protocol_type_id)
SELECT dm.id, pt.id
FROM device_model dm
CROSS JOIN common_code pt
INNER JOIN code_group cg ON cg.id = pt.group_id
WHERE dm.name = 'PDU-1P' AND dm.manufacturer = 'Raritan'
  AND cg.group_key = 'PROTOCOL_TYPE' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM device_model_protocol dmp
      WHERE dmp.model_id = dm.id AND dmp.protocol_type_id = pt.id
  );

-- -----------------------------------------------------------------------------
-- 3. 모델 SNMP point (4개)
-- -----------------------------------------------------------------------------

INSERT INTO device_model_snmp_point (model_protocol_id, name, oid, requires_instance, unit, enabled)
SELECT dmp.id, v.name, v.oid, 0, v.unit, 1
FROM device_model_protocol dmp
INNER JOIN device_model dm ON dm.id = dmp.model_id
INNER JOIN common_code pt ON pt.id = dmp.protocol_type_id
CROSS JOIN (
    SELECT 'AMP'       AS name, '1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.1' AS oid, 'A'   AS unit UNION ALL
    SELECT 'PF'        AS name, '1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.7' AS oid, NULL  AS unit UNION ALL
    SELECT 'TOTAL_WT'  AS name, '1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.5' AS oid, 'W'   AS unit UNION ALL
    SELECT 'TOTAL_KWH' AS name, '1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.8' AS oid, 'kWh' AS unit
) v
WHERE dm.name = 'PDU-1P' AND dm.manufacturer = 'Raritan' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM device_model_snmp_point p
      WHERE p.model_protocol_id = dmp.id AND p.name = v.name
  );

-- -----------------------------------------------------------------------------
-- 4. 장비 4대 (host 14.42.43.207, port 30263~30266)
-- -----------------------------------------------------------------------------

INSERT INTO devices (model_id, location_node_code, name, description, enabled)
SELECT dm.id, 'PD1RACK207', pdu.name, pdu.description, 1
FROM device_model dm
CROSS JOIN (
    SELECT 'PDU-1P-R30263' AS name, '14.42.43.207:30263 phase-1 Raritan PDU' AS description, 30263 AS port UNION ALL
    SELECT 'PDU-1P-R30264', '14.42.43.207:30264 phase-1 Raritan PDU', 30264 UNION ALL
    SELECT 'PDU-1P-R30265', '14.42.43.207:30265 phase-1 Raritan PDU', 30265 UNION ALL
    SELECT 'PDU-1P-R30266', '14.42.43.207:30266 phase-1 Raritan PDU', 30266
) pdu
WHERE dm.name = 'PDU-1P' AND dm.manufacturer = 'Raritan'
  AND NOT EXISTS (
      SELECT 1 FROM devices d
      WHERE d.location_node_code = 'PD1RACK207' AND d.name = pdu.name
  );

-- -----------------------------------------------------------------------------
-- 5. SNMP endpoint (community=public 은 spec 쪽 상수)
-- -----------------------------------------------------------------------------

INSERT INTO device_protocol_endpoint (device_id, protocol_type_id, host, port, enabled)
SELECT d.id, pt.id, '14.42.43.207', pdu.port, 1
FROM devices d
INNER JOIN device_model dm ON dm.id = d.model_id
CROSS JOIN common_code pt
INNER JOIN code_group cg ON cg.id = pt.group_id
CROSS JOIN (
    SELECT 'PDU-1P-R30263' AS name, 30263 AS port UNION ALL
    SELECT 'PDU-1P-R30264', 30264 UNION ALL
    SELECT 'PDU-1P-R30265', 30265 UNION ALL
    SELECT 'PDU-1P-R30266', 30266
) pdu
WHERE d.location_node_code = 'PD1RACK207'
  AND d.name = pdu.name
  AND dm.name = 'PDU-1P' AND dm.manufacturer = 'Raritan'
  AND cg.group_key = 'PROTOCOL_TYPE' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM device_protocol_endpoint e
      WHERE e.device_id = d.id AND e.protocol_type_id = pt.id
  );

-- -----------------------------------------------------------------------------
-- 6. 수집 Task + 1분 그룹 + 장비 4대 매핑
-- -----------------------------------------------------------------------------

INSERT INTO collection_task (name, model_id, script_type_id, active)
SELECT 'PDU-1P Raritan SNMP 수집', dm.id, pt.id, 1
FROM device_model dm
CROSS JOIN common_code pt
INNER JOIN code_group cg ON cg.id = pt.group_id
WHERE dm.name = 'PDU-1P' AND dm.manufacturer = 'Raritan'
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
WHERE dm.name = 'PDU-1P' AND dm.manufacturer = 'Raritan'
  AND cg.group_key = 'PROTOCOL_TYPE' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM collection_task_group g WHERE g.task_id = t.id
  );

INSERT INTO collection_task_device (group_id, device_id)
SELECT g.id, d.id
FROM collection_task_group g
INNER JOIN collection_task t ON t.id = g.task_id
INNER JOIN device_model dm ON dm.id = t.model_id
INNER JOIN devices d ON d.model_id = dm.id
WHERE dm.name = 'PDU-1P' AND dm.manufacturer = 'Raritan'
  AND d.location_node_code = 'PD1RACK207'
  AND d.name IN ('PDU-1P-R30263', 'PDU-1P-R30264', 'PDU-1P-R30265', 'PDU-1P-R30266')
  AND NOT EXISTS (
      SELECT 1 FROM collection_task_device m
      WHERE m.group_id = g.id AND m.device_id = d.id
  );

-- -----------------------------------------------------------------------------
-- 7. generated_spec (장비 4대 targets 포함)
--    Manager 재기동 시 repush·앱 재생성과 동일 JSON 형태
-- -----------------------------------------------------------------------------

UPDATE collection_task_group g
INNER JOIN collection_task t ON t.id = g.task_id
INNER JOIN device_model dm ON dm.id = t.model_id
INNER JOIN common_code pt ON pt.id = t.script_type_id
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
      '{"name":"AMP","template":"1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.1","requiresInstance":false},',
      '{"name":"PF","template":"1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.7","requiresInstance":false},',
      '{"name":"TOTAL_WT","template":"1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.5","requiresInstance":false},',
      '{"name":"TOTAL_KWH","template":"1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.8","requiresInstance":false}',
    '],',
    '"targets":[',
    IFNULL((
        SELECT GROUP_CONCAT(
            CONCAT(
                '{"deviceId":', d.id,
                ',"host":"', e.host,
                '","port":', e.port,
                ',"instanceId":null}'
            )
            ORDER BY e.port
            SEPARATOR ','
        )
        FROM devices d
        INNER JOIN device_protocol_endpoint e ON e.device_id = d.id
        INNER JOIN common_code ept ON ept.id = e.protocol_type_id
        INNER JOIN code_group ecg ON ecg.id = ept.group_id AND ecg.group_key = 'PROTOCOL_TYPE' AND ept.code = 'snmp'
        WHERE d.model_id = dm.id
          AND d.location_node_code = 'PD1RACK207'
          AND d.name IN ('PDU-1P-R30263', 'PDU-1P-R30264', 'PDU-1P-R30265', 'PDU-1P-R30266')
    ), ''),
    '],',
    '"skipped":[]',
    '}'
)
WHERE dm.name = 'PDU-1P' AND dm.manufacturer = 'Raritan';

-- -----------------------------------------------------------------------------
-- 확인
-- -----------------------------------------------------------------------------
-- SELECT dm.id AS model_id, d.id AS device_id, d.name, e.host, e.port
-- FROM device_model dm
-- JOIN devices d ON d.model_id = dm.id
-- JOIN device_protocol_endpoint e ON e.device_id = d.id
-- WHERE dm.name = 'PDU-1P' AND dm.manufacturer = 'Raritan'
-- ORDER BY e.port;
--
-- SELECT t.id AS task_id, g.id AS group_id, g.generated_spec
-- FROM collection_task t
-- JOIN collection_task_group g ON g.task_id = t.id
-- JOIN device_model dm ON dm.id = t.model_id
-- WHERE dm.name = 'PDU-1P' AND dm.manufacturer = 'Raritan';
--
-- GET /api/manager/collector/tasks?modelId={model_id}
-- =============================================================================
