-- GPU 에이전트 공통 모델 및 측정 항목. 반복 실행해도 기존 설정은 보존한다.
-- 적용 전 코드 그룹 DATA_POINT_TYPE, MODEL_TYPE, UNIT, PROTOCOL_TYPE이 필요하다.
CREATE TABLE IF NOT EXISTS gpu_agent_discovery (
 uuid CHAR(36) NOT NULL PRIMARY KEY,
 hostname VARCHAR(200) NOT NULL,
 metadata_json LONGTEXT NULL,
 first_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 last_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 device_id INT NULL,
 CONSTRAINT fk_gpu_agent_discovery_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO common_code (group_id, code, name, sort_order)
SELECT g.id, 'GPU_SERVER', 'GPU 서버', 100 FROM code_group g
WHERE g.group_key='MODEL_TYPE'
  AND NOT EXISTS (SELECT 1 FROM common_code c WHERE c.group_id=g.id AND c.code='GPU_SERVER');

INSERT INTO device_model (name, manufacturer, device_type_id, description)
SELECT 'SPATIVIA GPU Agent', 'SPATIVIA', c.id, 'GPU agent MQTT telemetry'
FROM common_code c JOIN code_group g ON g.id=c.group_id
WHERE g.group_key='MODEL_TYPE' AND c.code='GPU_SERVER'
  AND NOT EXISTS (SELECT 1 FROM device_model m WHERE m.name='SPATIVIA GPU Agent' AND m.manufacturer='SPATIVIA');

INSERT INTO device_model_protocol (model_id, protocol_type_id)
SELECT m.id, c.id FROM device_model m JOIN common_code c ON c.code='mqtt'
JOIN code_group g ON g.id=c.group_id AND g.group_key='PROTOCOL_TYPE'
WHERE m.name='SPATIVIA GPU Agent' AND m.manufacturer='SPATIVIA'
  AND NOT EXISTS (SELECT 1 FROM device_model_protocol p WHERE p.model_id=m.id AND p.protocol_type_id=c.id);

INSERT INTO device_model_mqtt_point
 (device_model_id, payload_field, point_name, data_point_type_id, unit_code_id,
  component_scope, component_type, component_path, component_index_field, component_uuid_field, enabled)
SELECT m.id, x.payload_field, x.point_name, t.id, u.id, x.scope,
 CASE WHEN x.scope='COMPONENT' THEN 'GPU' ELSE NULL END,
 CASE WHEN x.scope='COMPONENT' THEN 'components' ELSE '' END,
 CASE WHEN x.scope='COMPONENT' THEN 'index' ELSE NULL END,
 CASE WHEN x.scope='COMPONENT' THEN 'uuid' ELSE NULL END, 1
FROM device_model m
JOIN (
 SELECT 'object.cpu_utilization' payload_field, 'cpu_utilization' point_name, 'UNCLASSIFIED' type_code, 'PERCENT' unit_code, 'DEVICE' scope
 UNION ALL SELECT 'object.memory_used_GB','memory_used_GB','UNCLASSIFIED',NULL,'DEVICE'
 UNION ALL SELECT 'object.memory_total_GB','memory_total_GB','UNCLASSIFIED',NULL,'DEVICE'
 UNION ALL SELECT 'object.memory_utilization','memory_utilization','UNCLASSIFIED','PERCENT','DEVICE'
 UNION ALL SELECT 'object.uptime_seconds','uptime_seconds','UNCLASSIFIED',NULL,'DEVICE'
 UNION ALL SELECT 'object.gpu_count','gpu_count','UNCLASSIFIED',NULL,'DEVICE'
 UNION ALL SELECT 'object.gpu_total_power','gpu_total_power','POWER','W','DEVICE'
 UNION ALL SELECT 'object.load_avg_1min','load_avg_1min','UNCLASSIFIED',NULL,'DEVICE'
 UNION ALL SELECT 'object.load_avg_5min','load_avg_5min','UNCLASSIFIED',NULL,'DEVICE'
 UNION ALL SELECT 'object.load_avg_15min','load_avg_15min','UNCLASSIFIED',NULL,'DEVICE'
 UNION ALL SELECT 'object.gpu_temperature','gpu_temperature','TEMPERATURE','CELSIUS','COMPONENT'
 UNION ALL SELECT 'object.power','power','POWER','W','COMPONENT'
 UNION ALL SELECT 'object.gpu_utilization','gpu_utilization','UNCLASSIFIED','PERCENT','COMPONENT'
 UNION ALL SELECT 'object.vram_utilization','vram_utilization','UNCLASSIFIED','PERCENT','COMPONENT'
 UNION ALL SELECT 'object.memory_used_MiB','memory_used_MiB','UNCLASSIFIED',NULL,'COMPONENT'
 UNION ALL SELECT 'object.memory_total_MiB','memory_total_MiB','UNCLASSIFIED',NULL,'COMPONENT'
 UNION ALL SELECT 'object.fan_speed','fan_speed','UNCLASSIFIED','PERCENT','COMPONENT'
) x
JOIN code_group tg ON tg.group_key='DATA_POINT_TYPE'
JOIN common_code t ON t.group_id=tg.id AND t.code=x.type_code
LEFT JOIN code_group ug ON ug.group_key='UNIT'
LEFT JOIN common_code u ON u.group_id=ug.id AND u.code=x.unit_code
WHERE m.name='SPATIVIA GPU Agent' AND m.manufacturer='SPATIVIA'
 AND NOT EXISTS (SELECT 1 FROM device_model_mqtt_point p WHERE p.device_model_id=m.id AND p.point_name=x.point_name);

