-- Catch-up migrations for databases baselined at V1. All DDL is additive and re-run-safe.

-- BEGIN 20261106_gpu_agent_catalog.sql
-- GPU 에이전트 공통 모델 및 측정 항목. 반복 실행해도 기존 설정은 보존한다.
-- 적용 전 코드 그룹 DATA_POINT_TYPE, MODEL_TYPE, UNIT, PROTOCOL_TYPE이 필요하다.
CREATE TABLE IF NOT EXISTS gpu_agent_discovery (
 uuid CHAR(36) NOT NULL PRIMARY KEY COMMENT '에이전트 UUID',
 hostname VARCHAR(200) NOT NULL COMMENT '에이전트 호스트 이름',
 metadata_json LONGTEXT NULL COMMENT '에이전트 메타데이터 JSON',
 first_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최초 발견 시각',
 last_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최근 발견 시각',
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '승인 상태',
 device_id INT NULL COMMENT '연결된 장비 ID',
 CONSTRAINT fk_gpu_agent_discovery_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GPU 에이전트 발견 정보';
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
-- END 20261106_gpu_agent_catalog.sql

-- BEGIN 20261106_scene_device_connection_history.sql
-- 장비와 도면 설비의 연결·해제 이력 (기존 배치 이력과 별도)
CREATE TABLE IF NOT EXISTS scene_device_connection_history (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '연결 이력 ID',
  device_id INT NOT NULL COMMENT '장비 ID',
  page_code VARCHAR(100) NOT NULL COMMENT '도면 페이지 코드',
  previous_host VARCHAR(255) NULL COMMENT '변경 전 연결 위치',
  current_host VARCHAR(255) NULL COMMENT '변경 후 연결 위치',
  actor_name VARCHAR(255) NULL COMMENT '변경한 사용자',
  changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '변경 시각',
  INDEX idx_scene_connection_device_time (device_id, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='도면 장비 연결·해제 이력';
-- END 20261106_scene_device_connection_history.sql

-- BEGIN 20261109_panel_photo.sql
ALTER TABLE `device_page_scene_item` ADD COLUMN IF NOT EXISTS `panel_photo` longtext DEFAULT NULL COMMENT '분전반 사진 데이터';
-- END 20261109_panel_photo.sql

-- BEGIN 20261109_panel_editable_layout.sql
ALTER TABLE `device_page_scene_item` ADD COLUMN IF NOT EXISTS `panel_layout_initialized` tinyint(1) NOT NULL DEFAULT 0 COMMENT '분전반 편집 레이아웃 초기화 여부';
-- END 20261109_panel_editable_layout.sql

-- BEGIN 20261109_accura_2350_scene_models.sql
-- Keep existing administrator-managed models unchanged.
INSERT IGNORE INTO `scene_model`
  (`model_key`, `model_name`, `category`, `builtin_file`, `file_name`, `width_m`, `height_m`, `depth_m`)
VALUES
  ('SR_ACCURA_2350_1P', 'ACCURA 2350 단상', '센서', 'accura_2350_1p.glb', 'accura_2350_1p.glb', .045, .105, .045),
  ('SR_ACCURA_2350_3P', 'ACCURA 2350 3상', '센서', 'accura_2350_3p.glb', 'accura_2350_3p.glb', .060, .105, .045);
-- END 20261109_accura_2350_scene_models.sql

-- BEGIN 20261109_scene_model_panel_installable.sql
ALTER TABLE `scene_model` ADD COLUMN IF NOT EXISTS `panel_installable` tinyint(1) NOT NULL DEFAULT 0 COMMENT '분전반 내부 설치 가능 여부';
UPDATE `scene_model` SET `panel_installable` = 1 WHERE `model_key` IN ('SR_ACCURA_2350_1P','SR_ACCURA_2350_3P');
-- END 20261109_scene_model_panel_installable.sql

-- BEGIN 20261109_panel_breaker_model.sql
INSERT IGNORE INTO `scene_model`
  (`model_key`, `model_name`, `category`, `builtin_file`, `file_name`, `width_m`, `height_m`, `depth_m`, `panel_installable`)
VALUES ('SR_PANEL_BREAKER', '분전반 차단기', '전력 설비', 'panel_breaker_3p.glb', 'panel_breaker_3p.glb', .095, .11, .075, 1);
-- END 20261109_panel_breaker_model.sql

-- BEGIN 20261109_panel_default_models.sql
INSERT IGNORE INTO `scene_model` (`model_key`,`model_name`,`category`,`builtin_file`,`file_name`,`width_m`,`height_m`,`depth_m`,`panel_installable`) VALUES
('SR_PANEL_BRANCH','분전반 분기 차단기','전력 설비','panel_breaker_3p.glb','panel_breaker_3p.glb',.34,.14,.12,1),
('SR_PANEL_MAIN','분전반 메인 차단기','전력 설비','panel_main_breaker.glb','panel_main_breaker.glb',.37,.31,.13,1),
('SR_PANEL_METER','분전반 계기','전력 설비','panel_meter.glb','panel_meter.glb',.36,.23,.12,1),
('SR_PANEL_BUSBAR','분전반 부스바','전력 설비','panel_busbar.glb','panel_busbar.glb',.06,.29,.04,1),
('SR_PANEL_WIRE','분전반 배선','전력 설비','panel_wire.glb','panel_wire.glb',.08,.13,.012,1);
-- END 20261109_panel_default_models.sql

-- BEGIN 20261110_widget_trend_defaults.sql
-- 배포 시 매번 다시 실행되므로(.github/workflows/deploy.yml) 재실행해도 안전하게 IF NOT EXISTS를 쓴다.
ALTER TABLE page_widget
    ADD COLUMN IF NOT EXISTS trend_range_preset VARCHAR(24) NOT NULL DEFAULT 'last_3d' COMMENT '추세 조회 기간 사전 설정',
    ADD COLUMN IF NOT EXISTS trend_window VARCHAR(8) NOT NULL DEFAULT '15m' COMMENT '추세 집계 간격';
-- END 20261110_widget_trend_defaults.sql

-- BEGIN 20261110_scene_model_role.sql
-- 3D 모델 역할을 모델 키 이름 규칙(SR_*RACK, SR_INROW_PANEL 등) 대신 카탈로그 값으로 관리한다.
-- 최초 적용 및 중단 복구 전용. 성공 후 반복 실행하지 않는다: 의도한 NULL이 기본 역할로 바뀔 수 있다.
-- T-002 Flyway V2에서 적용하며 이 파일이 포함되지 않은 기존 배포 방식으로 운영에 배포하지 않는다.
ALTER TABLE `scene_model` ADD COLUMN IF NOT EXISTS `scene_role` varchar(20) DEFAULT NULL COMMENT '도면 역할: RACK, PANEL, FRAME 또는 NULL';
UPDATE `scene_model` SET `scene_role` = 'RACK' WHERE `scene_role` IS NULL AND `model_key` IN ('SR_RACK','SR_DOUBLE_RACK','SR_DLC_RACK','SR_INROW_RACK','SR_OCP_RACK','SR_RDC_RACK','SR_ORV3_RACK','SR_HAC_RACK');
UPDATE `scene_model` SET `scene_role` = 'PANEL' WHERE `scene_role` IS NULL AND `model_key` = 'SR_INROW_PANEL';
UPDATE `scene_model` SET `scene_role` = 'FRAME' WHERE `scene_role` IS NULL AND `model_key` IN ('SR_INROW_MOLD','SR_WALL_COIL');
-- END 20261110_scene_model_role.sql

-- BEGIN 20261110_initial_admin_lock.sql
-- Stable row serializes first administrator registration across Manager instances.
CREATE TABLE IF NOT EXISTS `initial_admin_lock` (
  `id` int NOT NULL PRIMARY KEY COMMENT '초기 관리자 등록 잠금 키'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='초기 관리자 등록 직렬화 잠금';
INSERT IGNORE INTO `initial_admin_lock` (`id`) VALUES (1);
-- END 20261110_initial_admin_lock.sql

-- JDBC-only tables are included here so a baseline database is brought up to date.
CREATE TABLE IF NOT EXISTS rack_widget_config (
  rack_location_code VARCHAR(10) NOT NULL PRIMARY KEY COMMENT '랙 위치 코드',
  widgets_json LONGTEXT NOT NULL COMMENT '랙 위젯 설정 JSON',
  updated_dt TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',
  CONSTRAINT fk_rack_widget_config_location FOREIGN KEY (rack_location_code) REFERENCES location_node(code) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='랙 위젯 설정';

CREATE TABLE IF NOT EXISTS rack_scene_display_config (
  rack_location_code VARCHAR(10) NOT NULL PRIMARY KEY COMMENT '랙 위치 코드',
  display_json LONGTEXT NOT NULL COMMENT '랙 도면 표시 설정 JSON',
  updated_dt TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',
  CONSTRAINT fk_rack_scene_display_location FOREIGN KEY (rack_location_code) REFERENCES location_node(code) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='랙 도면 표시 설정';

CREATE TABLE IF NOT EXISTS scene_device_widget_config (
  device_id INT NOT NULL PRIMARY KEY COMMENT '장비 ID',
  widgets_json LONGTEXT NOT NULL COMMENT '장비 도면 위젯 설정 JSON',
  updated_dt TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',
  CONSTRAINT fk_scene_widget_device FOREIGN KEY (device_id) REFERENCES devices(id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='도면 장비 위젯 설정';
