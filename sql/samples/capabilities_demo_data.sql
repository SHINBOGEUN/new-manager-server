-- =============================================================================
-- capabilities 데모 데이터 (샘플 INSERT)
-- =============================================================================
-- 작성일  : 2026-08-14
-- 대상 DB : MariaDB (dcim_new)
--
-- 적용 방법 (예시):
--   mysql -h HOST -P PORT -u dcim -p dcim_new < sql/samples/capabilities_demo_data.sql
--
-- 선행 조건: V001 ~ V014 (특히 V004 location, V005 model, V014 DEVICE_PAGE)
--
-- 설계 문서: docs/device/DEVICE_CAPABILITY_API.md
--
-- 역할:
--   - GET /api/manager/devices/capabilities 호출 예시를 위한 데모 데이터
--   - 재실행 안전 (이미 있으면 INSERT 생략)
--
-- 확인 예:
--   GET /api/manager/devices/capabilities?pageCode=ENVIRONMENT&locationNodeCode=DEMOZONE01&includeSubtree=true
--
-- 시나리오:
--   DEMOZONE01 (1층 Zone-A)
--     └── DEMORACK01 (Rack-01)
--           ├── PDU-DEMO-L   (AP8959 PDU, ENVIRONMENT+POWER, instance OID)
--           └── SENSOR-DEMO  (LHT65N 센서, ENVIRONMENT, 고정 OID)
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
-- 5. 장비 ↔ 페이지 매핑
-- -----------------------------------------------------------------------------

INSERT INTO device_page (device_id, page_code_id)
SELECT d.id, cc.id
FROM devices d
CROSS JOIN common_code cc
INNER JOIN code_group cg ON cg.id = cc.group_id
WHERE d.location_node_code = 'DEMORACK01' AND d.name = 'PDU-DEMO-L'
  AND cg.group_key = 'DEVICE_PAGE' AND cc.code = 'ENVIRONMENT'
  AND NOT EXISTS (
      SELECT 1 FROM device_page dp
      WHERE dp.device_id = d.id AND dp.page_code_id = cc.id
  );

INSERT INTO device_page (device_id, page_code_id)
SELECT d.id, cc.id
FROM devices d
CROSS JOIN common_code cc
INNER JOIN code_group cg ON cg.id = cc.group_id
WHERE d.location_node_code = 'DEMORACK01' AND d.name = 'PDU-DEMO-L'
  AND cg.group_key = 'DEVICE_PAGE' AND cc.code = 'POWER'
  AND NOT EXISTS (
      SELECT 1 FROM device_page dp
      WHERE dp.device_id = d.id AND dp.page_code_id = cc.id
  );

INSERT INTO device_page (device_id, page_code_id)
SELECT d.id, cc.id
FROM devices d
CROSS JOIN common_code cc
INNER JOIN code_group cg ON cg.id = cc.group_id
WHERE d.location_node_code = 'DEMORACK01' AND d.name = 'SENSOR-DEMO'
  AND cg.group_key = 'DEVICE_PAGE' AND cc.code = 'ENVIRONMENT'
  AND NOT EXISTS (
      SELECT 1 FROM device_page dp
      WHERE dp.device_id = d.id AND dp.page_code_id = cc.id
  );

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
