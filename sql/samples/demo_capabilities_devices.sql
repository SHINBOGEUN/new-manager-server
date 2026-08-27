-- =============================================================================
-- demo_capabilities_devices: capabilities API 데모 (위치/모델/장비/위젯/endpoint)
-- =============================================================================
-- 작성일  : 2026-08-26 (기존 capabilities_demo + 수집 스펙 확인 SELECT 통합)
-- 대상 DB : MariaDB (dcim_new)
--
-- 적용 방법:
--   mysql -h HOST -P PORT -u dcim -p dcim_new < sql/samples/demo_capabilities_devices.sql
--
-- 선행: history + seed/REQUIRED_BOOTSTRAP
-- 설계: docs/device/DEVICE_CAPABILITY_API.md
--
-- 시나리오:
--   DEMOZONE01 → DEMORACK01
--     ├── PDU-DEMO-L   (AP8959, instance OID)
--     └── SENSOR-DEMO  (LHT65N, 고정 OID)
--
-- 확인 예:
--   GET /api/manager/devices/capabilities?pageCode=ENVIRONMENT&locationNodeCode=DEMOZONE01&includeSubtree=true
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 위치 트리
-- -----------------------------------------------------------------------------

INSERT INTO location_node (code, parent_code, location_type_id, name)
SELECT 'DEMOZONE01', NULL, cc.id, '1층 Zone-A (Demo)'
FROM common_code cc
INNER JOIN code_group cg ON cg.id = cc.group_id
WHERE cg.group_key = 'LOCATION_TYPE'
  AND cc.code = 'ZONE'
  AND NOT EXISTS (SELECT 1 FROM location_node WHERE code = 'DEMOZONE01');

INSERT INTO location_node (code, parent_code, location_type_id, name)
SELECT 'DEMORACK01', 'DEMOZONE01', cc.id, 'Demo-Rack-01'
FROM common_code cc
INNER JOIN code_group cg ON cg.id = cc.group_id
WHERE cg.group_key = 'LOCATION_TYPE'
  AND cc.code = 'RACK'
  AND NOT EXISTS (SELECT 1 FROM location_node WHERE code = 'DEMORACK01');

-- -----------------------------------------------------------------------------
-- 2. 장비 모델 (PDU / 센서)
-- -----------------------------------------------------------------------------

INSERT INTO device_model (name, manufacturer, device_type_id, description)
SELECT 'AP8959-DEMO', 'APC', dt.id, 'Capabilities 데모 PDU'
FROM common_code dt
INNER JOIN code_group cg ON cg.id = dt.group_id
WHERE cg.group_key = 'MODEL_TYPE'
  AND dt.code = 'PDU'
  AND NOT EXISTS (
      SELECT 1 FROM device_model WHERE name = 'AP8959-DEMO' AND manufacturer = 'APC'
  );

INSERT INTO device_model (name, manufacturer, device_type_id, description)
SELECT 'LHT65N-DEMO', 'Dragino', dt.id, 'Capabilities 데모 온습도 센서'
FROM common_code dt
INNER JOIN code_group cg ON cg.id = dt.group_id
WHERE cg.group_key = 'MODEL_TYPE'
  AND dt.code = 'SENSOR'
  AND NOT EXISTS (
      SELECT 1 FROM device_model WHERE name = 'LHT65N-DEMO' AND manufacturer = 'Dragino'
  );

INSERT INTO device_model_protocol (model_id, protocol_type_id)
SELECT dm.id, pt.id
FROM device_model dm
CROSS JOIN common_code pt
INNER JOIN code_group cg ON cg.id = pt.group_id
WHERE dm.name = 'AP8959-DEMO' AND dm.manufacturer = 'APC'
  AND cg.group_key = 'PROTOCOL_TYPE' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM device_model_protocol dmp
      WHERE dmp.model_id = dm.id AND dmp.protocol_type_id = pt.id
  );

INSERT INTO device_model_protocol (model_id, protocol_type_id)
SELECT dm.id, pt.id
FROM device_model dm
CROSS JOIN common_code pt
INNER JOIN code_group cg ON cg.id = pt.group_id
WHERE dm.name = 'LHT65N-DEMO' AND dm.manufacturer = 'Dragino'
  AND cg.group_key = 'PROTOCOL_TYPE' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM device_model_protocol dmp
      WHERE dmp.model_id = dm.id AND dmp.protocol_type_id = pt.id
  );

-- -----------------------------------------------------------------------------
-- 3. 모델 SNMP point
-- -----------------------------------------------------------------------------

INSERT INTO device_model_snmp_point (model_protocol_id, name, oid, requires_instance, unit, enabled)
SELECT dmp.id, 'V', '1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.{instanceId}.3', 1, 'V', 1
FROM device_model_protocol dmp
INNER JOIN device_model dm ON dm.id = dmp.model_id
INNER JOIN common_code pt ON pt.id = dmp.protocol_type_id
WHERE dm.name = 'AP8959-DEMO' AND dm.manufacturer = 'APC' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM device_model_snmp_point p
      WHERE p.model_protocol_id = dmp.id AND p.name = 'V'
  );

INSERT INTO device_model_snmp_point (model_protocol_id, name, oid, requires_instance, unit, enabled)
SELECT dmp.id, 'A', '1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.{instanceId}.4', 1, 'A', 1
FROM device_model_protocol dmp
INNER JOIN device_model dm ON dm.id = dmp.model_id
INNER JOIN common_code pt ON pt.id = dmp.protocol_type_id
WHERE dm.name = 'AP8959-DEMO' AND dm.manufacturer = 'APC' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM device_model_snmp_point p
      WHERE p.model_protocol_id = dmp.id AND p.name = 'A'
  );

INSERT INTO device_model_snmp_point (model_protocol_id, name, oid, requires_instance, unit, enabled)
SELECT dmp.id, 'temp', '1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.1.0', 0, 'C', 1
FROM device_model_protocol dmp
INNER JOIN device_model dm ON dm.id = dmp.model_id
INNER JOIN common_code pt ON pt.id = dmp.protocol_type_id
WHERE dm.name = 'LHT65N-DEMO' AND dm.manufacturer = 'Dragino' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM device_model_snmp_point p
      WHERE p.model_protocol_id = dmp.id AND p.name = 'temp'
  );

INSERT INTO device_model_snmp_point (model_protocol_id, name, oid, requires_instance, unit, enabled)
SELECT dmp.id, 'hum', '1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.2.0', 0, '%', 1
FROM device_model_protocol dmp
INNER JOIN device_model dm ON dm.id = dmp.model_id
INNER JOIN common_code pt ON pt.id = dmp.protocol_type_id
WHERE dm.name = 'LHT65N-DEMO' AND dm.manufacturer = 'Dragino' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM device_model_snmp_point p
      WHERE p.model_protocol_id = dmp.id AND p.name = 'hum'
  );

-- -----------------------------------------------------------------------------
-- 4. 장비 인스턴스
-- -----------------------------------------------------------------------------

INSERT INTO devices (model_id, location_node_code, name, description, enabled)
SELECT dm.id, 'DEMORACK01', 'PDU-DEMO-L', 'Demo Rack-01 좌측 PDU', 1
FROM device_model dm
WHERE dm.name = 'AP8959-DEMO' AND dm.manufacturer = 'APC'
  AND NOT EXISTS (
      SELECT 1 FROM devices d
      WHERE d.location_node_code = 'DEMORACK01' AND d.name = 'PDU-DEMO-L'
  );

INSERT INTO devices (model_id, location_node_code, name, description, enabled)
SELECT dm.id, 'DEMORACK01', 'SENSOR-DEMO', 'Demo Rack-01 온습도 센서', 1
FROM device_model dm
WHERE dm.name = 'LHT65N-DEMO' AND dm.manufacturer = 'Dragino'
  AND NOT EXISTS (
      SELECT 1 FROM devices d
      WHERE d.location_node_code = 'DEMORACK01' AND d.name = 'SENSOR-DEMO'
  );

-- -----------------------------------------------------------------------------
-- 5. (선택) 페이지 위젯에 장비 연결 — capabilities pageCode 필터용
--    device_page 테이블은 V018에서 제거됨. page_widget_device 사용.
-- -----------------------------------------------------------------------------

SET @env_page_id := (
    SELECT cc.id FROM common_code cc
    INNER JOIN code_group cg ON cg.id = cc.group_id
    WHERE cg.group_key = 'DEVICE_PAGE' AND cc.code = 'ENVIRONMENT'
    LIMIT 1
);
SET @power_page_id := (
    SELECT cc.id FROM common_code cc
    INNER JOIN code_group cg ON cg.id = cc.group_id
    WHERE cg.group_key = 'DEVICE_PAGE' AND cc.code = 'POWER'
    LIMIT 1
);

INSERT INTO page_widget (page_code_id, name, enabled, query_kind)
SELECT @env_page_id, '데모 ENVIRONMENT', 1, 'last' FROM DUAL
WHERE @env_page_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM page_widget
      WHERE page_code_id = @env_page_id AND name = '데모 ENVIRONMENT'
  );

INSERT INTO page_widget (page_code_id, name, enabled, query_kind)
SELECT @power_page_id, '데모 POWER', 1, 'last' FROM DUAL
WHERE @power_page_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM page_widget
      WHERE page_code_id = @power_page_id AND name = '데모 POWER'
  );

INSERT IGNORE INTO page_widget_point (widget_id, point_name)
SELECT w.id, p.point_name
FROM page_widget w
INNER JOIN (
    SELECT '데모 ENVIRONMENT' AS name, 'V' AS point_name
    UNION ALL SELECT '데모 ENVIRONMENT', 'temp'
    UNION ALL SELECT '데모 POWER', 'V'
) p ON p.name = w.name
WHERE w.page_code_id IN (@env_page_id, @power_page_id);

INSERT IGNORE INTO page_widget_device (widget_id, device_id)
SELECT w.id, d.id
FROM page_widget w
INNER JOIN devices d ON d.location_node_code = 'DEMORACK01'
WHERE w.page_code_id = @env_page_id AND w.name = '데모 ENVIRONMENT'
  AND d.name IN ('PDU-DEMO-L', 'SENSOR-DEMO');

INSERT IGNORE INTO page_widget_device (widget_id, device_id)
SELECT w.id, d.id
FROM page_widget w
INNER JOIN devices d ON d.location_node_code = 'DEMORACK01' AND d.name = 'PDU-DEMO-L'
WHERE w.page_code_id = @power_page_id AND w.name = '데모 POWER';

-- -----------------------------------------------------------------------------
-- 6. SNMP endpoint (host/port)
-- -----------------------------------------------------------------------------

INSERT INTO device_protocol_endpoint (device_id, protocol_type_id, host, port, enabled)
SELECT d.id, pt.id, '192.168.1.10', 161, 1
FROM devices d
CROSS JOIN common_code pt
INNER JOIN code_group cg ON cg.id = pt.group_id
WHERE d.location_node_code = 'DEMORACK01' AND d.name = 'PDU-DEMO-L'
  AND cg.group_key = 'PROTOCOL_TYPE' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM device_protocol_endpoint e
      WHERE e.device_id = d.id AND e.protocol_type_id = pt.id
  );

INSERT INTO device_protocol_endpoint (device_id, protocol_type_id, host, port, enabled)
SELECT d.id, pt.id, '192.168.1.20', 161, 1
FROM devices d
CROSS JOIN common_code pt
INNER JOIN code_group cg ON cg.id = pt.group_id
WHERE d.location_node_code = 'DEMORACK01' AND d.name = 'SENSOR-DEMO'
  AND cg.group_key = 'PROTOCOL_TYPE' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM device_protocol_endpoint e
      WHERE e.device_id = d.id AND e.protocol_type_id = pt.id
  );

-- -----------------------------------------------------------------------------
-- 7. SNMP instance (PDU만 — {instanceId} 치환)
-- -----------------------------------------------------------------------------

INSERT INTO device_snmp_instance (endpoint_id, instance_id)
SELECT e.id, 1
FROM device_protocol_endpoint e
INNER JOIN devices d ON d.id = e.device_id
INNER JOIN common_code pt ON pt.id = e.protocol_type_id
WHERE d.location_node_code = 'DEMORACK01' AND d.name = 'PDU-DEMO-L' AND pt.code = 'snmp'
  AND NOT EXISTS (
      SELECT 1 FROM device_snmp_instance si WHERE si.endpoint_id = e.id
  );

-- =============================================================================
-- 기대 결과 (capabilities API)
-- =============================================================================
--
-- GET /api/manager/devices/capabilities?pageCode=ENVIRONMENT&locationNodeCode=DEMOZONE01&includeSubtree=true
--
-- → PDU-DEMO-L
--     endpoint: 192.168.1.10:161, instanceId=1
--     points:
--       V → resolvedOid: 1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.1.3
--       A → resolvedOid: 1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.1.4
-- → SENSOR-DEMO
--     endpoint: 192.168.1.20:161
--     points:
--       temp → resolvedOid: 1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.1.0
--       hum  → resolvedOid: 1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.2.0
--
-- GET ...?pageCode=POWER&locationNodeCode=DEMORACK01
-- → PDU-DEMO-L 만 (2 points)
-- =============================================================================

-- =============================================================================
-- 확인 SELECT (수집 스펙 재료)
-- =============================================================================

-- 1. Task 등록에 필요한 scriptTypeId
-- -----------------------------------------------------------------------------
-- POST /api/manager/collector/tasks 의 scriptTypeId 로 사용

SELECT cc.id   AS scriptTypeId,
       cc.code AS scriptTypeCode,
       cc.name AS scriptTypeName
FROM common_code cc
INNER JOIN code_group cg ON cg.id = cc.group_id
WHERE cg.group_key = 'PROTOCOL_TYPE'
  AND cc.code = 'snmp';

-- -----------------------------------------------------------------------------
-- 2. 스크립트 생성 재료 (장비 + endpoint + resolved OID)
-- -----------------------------------------------------------------------------
-- 이 SELECT 결과가 나중에 CollectionGroupSpecService 입력이 됩니다.

SELECT
    d.id                                              AS device_id,
    d.name                                            AS device_name,
    e.host,
    e.port,
    si.instance_id,
    p.name                                            AS point_name,
    p.oid                                             AS oid_template,
    p.requires_instance,
    CASE
        WHEN p.requires_instance = 0 THEN p.oid
        WHEN si.instance_id IS NULL THEN NULL
        ELSE REPLACE(p.oid, '{instanceId}', CAST(si.instance_id AS CHAR))
    END                                               AS resolved_oid
FROM devices d
INNER JOIN device_model dm
        ON dm.id = d.model_id
INNER JOIN device_model_protocol dmp
        ON dmp.model_id = dm.id
INNER JOIN common_code pt
        ON pt.id = dmp.protocol_type_id
INNER JOIN code_group ptg
        ON ptg.id = pt.group_id
       AND ptg.group_key = 'PROTOCOL_TYPE'
       AND pt.code = 'snmp'
INNER JOIN device_model_snmp_point p
        ON p.model_protocol_id = dmp.id
       AND p.enabled = 1
LEFT JOIN device_protocol_endpoint e
       ON e.device_id = d.id
      AND e.protocol_type_id = pt.id
      AND e.enabled = 1
LEFT JOIN device_snmp_instance si
       ON si.endpoint_id = e.id
WHERE d.enabled = 1
  AND d.location_node_code = 'DEMORACK01'
  AND d.name IN ('PDU-DEMO-L', 'SENSOR-DEMO')
ORDER BY d.id, p.id;

-- 기대 결과:
--   PDU-DEMO-L  192.168.1.10 161  1  V     ...{instanceId}.3  →  ...1.3
--   PDU-DEMO-L  192.168.1.10 161  1  A     ...{instanceId}.4  →  ...1.4
--   SENSOR-DEMO 192.168.1.20 161     temp  고정 OID           →  그대로
--   SENSOR-DEMO 192.168.1.20 161     hum   고정 OID           →  그대로

-- -----------------------------------------------------------------------------
-- 3. 데모 데이터가 없을 때 확인
-- -----------------------------------------------------------------------------
SELECT
    (SELECT COUNT(*) FROM devices WHERE name IN ('PDU-DEMO-L', 'SENSOR-DEMO')) AS device_count,
    (SELECT COUNT(*) FROM device_protocol_endpoint e
     INNER JOIN devices d ON d.id = e.device_id
     WHERE d.name IN ('PDU-DEMO-L', 'SENSOR-DEMO')) AS endpoint_count,
    (SELECT COUNT(*) FROM device_model_snmp_point p
     INNER JOIN device_model_protocol dmp ON dmp.id = p.model_protocol_id
     INNER JOIN device_model dm ON dm.id = dmp.model_id
     WHERE dm.name IN ('AP8959-DEMO', 'LHT65N-DEMO')) AS point_count;

-- device_count = 2, endpoint_count = 2, point_count = 4 이어야 합니다.
-- 아니면 이 파일의 INSERT 구간을 먼저 실행하세요.
