-- =============================================================================
-- 수집 스크립트 생성용 데모 데이터 + 확인 쿼리
-- =============================================================================
-- 작성일  : 2026-08-18
-- 대상 DB : MariaDB (dcim_new)
--
-- 적용 방법:
--   1) V001 ~ V016 적용
--   2) mysql ... < sql/samples/capabilities_demo_data.sql   (장비/OID 데모)
--   3) mysql ... < sql/samples/collection_task_script_demo_data.sql  (확인)
--
-- 역할:
--   - spec 생성기가 읽는 레코드 체인을 한 번에 확인
--   - Task 등록 API에 넣을 scriptTypeId 조회
--   - collection_task 자체는 INSERT 하지 않음 (API로 등록)
--
-- 주의:
--   POST /api/manager/collector/tasks 는 모델+그룹으로 생성하고
--   collection_task_group.generated_spec 에 JSON spec 을 저장합니다.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. 스크립트 생성기가 보는 레코드 체인
-- -----------------------------------------------------------------------------
--
-- common_code (PROTOCOL_TYPE.snmp)
--   └── device_model_protocol
--         ├── device_model_snmp_point   (OID 템플릿)
--         └── devices
--               ├── device_protocol_endpoint  (host, port)
--               └── device_snmp_instance      ({instanceId} 치환, 필요 시)
--
-- 데모 시나리오 (capabilities_demo_data.sql):
--   DEMORACK01
--     ├── PDU-DEMO-L    192.168.1.10:161  instanceId=1  point: V, A
--     └── SENSOR-DEMO   192.168.1.20:161  고정 OID       point: temp, hum
-- -----------------------------------------------------------------------------

-- -----------------------------------------------------------------------------
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
-- 아니면 먼저 sql/samples/capabilities_demo_data.sql 을 실행하세요.
