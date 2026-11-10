-- Empty-schema baseline. Existing installations are baselined at version 1 and start at V2.
-- Source: sql/schema (excluding 43, 44-47, 51), sql/seed/model_catalog.sql,
-- sql/seed/device_monitoring_mode.sql, and the DATA_POINT_TYPE codes formerly bootstrapped at runtime.

-- BEGIN 01_users.sql
-- 01_users.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '사용자 ID',
  `username` varchar(255) NOT NULL COMMENT '로그인 아이디',
  `password` varchar(255) NOT NULL COMMENT 'BCrypt 해시 비밀번호',
  `role` varchar(50) DEFAULT NULL COMMENT '사용자 구분 (ADMIN, USER, GUEST)',
  `refresh_token` varchar(512) DEFAULT NULL COMMENT 'JWT refresh token',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자'
-- END 01_users.sql

-- BEGIN 02_code_group.sql
-- 02_code_group.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `code_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '코드 그룹 ID',
  `group_key` varchar(100) NOT NULL COMMENT '그룹 키 (예: DEVICE_TYPE)',
  `group_name` varchar(255) NOT NULL COMMENT '그룹 표시명',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_group_group_key` (`group_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공통 코드 그룹'
-- END 02_code_group.sql

-- BEGIN 03_common_code.sql
-- 03_common_code.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `common_code` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '공통 코드 ID',
  `group_id` int(11) NOT NULL COMMENT '코드 그룹 ID (FK)',
  `code` varchar(100) NOT NULL COMMENT '코드 값 (예: ups, pdu)',
  `name` varchar(255) NOT NULL COMMENT '코드 표시명',
  `sort_order` int(11) DEFAULT NULL COMMENT '정렬 순서',
  `threshold_supported` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'DATA_POINT_TYPE 임계값 지원 여부',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_common_code_group_id_code` (`group_id`,`code`),
  KEY `idx_common_code_group_id` (`group_id`),
  CONSTRAINT `fk_common_code_group_id` FOREIGN KEY (`group_id`) REFERENCES `code_group` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공통 코드'
-- END 03_common_code.sql

-- BEGIN 04_location_node.sql
-- 04_location_node.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `location_node` (
  `code` char(10) NOT NULL COMMENT '노드 PK (일반: 10자 Base62 / 시스템: UNASSIGNED)',
  `parent_code` char(10) DEFAULT NULL COMMENT '부모 노드 code (루트는 NULL)',
  `location_type_id` int(11) NOT NULL COMMENT '위치 유형 ID (FK → common_code, LOCATION_TYPE만)',
  `name` varchar(255) NOT NULL COMMENT '노드 표시명',
  `rack_u_capacity` int(11) DEFAULT NULL COMMENT 'Rack 최대 U 용량 (Rack 외 위치는 NULL)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`code`),
  UNIQUE KEY `uk_location_node_parent_code_name` (`parent_code`,`name`),
  KEY `idx_location_node_parent_code` (`parent_code`),
  KEY `idx_location_node_location_type_id` (`location_type_id`),
  CONSTRAINT `fk_location_node_location_type_id` FOREIGN KEY (`location_type_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_location_node_parent_code` FOREIGN KEY (`parent_code`) REFERENCES `location_node` (`code`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='위치 트리 노드'
-- END 04_location_node.sql

-- BEGIN 05_device_model.sql
-- 05_device_model.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `device_model` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '장비 모델 ID',
  `name` varchar(255) NOT NULL COMMENT '모델/제품명',
  `manufacturer` varchar(255) NOT NULL COMMENT '제조사',
  `device_type_id` int(11) NOT NULL COMMENT 'common_code.id (모델 유형)',
  `description` varchar(1000) DEFAULT NULL COMMENT '설명',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_model_name_manufacturer` (`name`,`manufacturer`),
  KEY `idx_device_model_device_type_id` (`device_type_id`),
  CONSTRAINT `fk_device_model_device_type_id` FOREIGN KEY (`device_type_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 제품 모델 (SKU/제품군)'
-- END 05_device_model.sql

-- BEGIN 06_device_model_protocol.sql
-- 06_device_model_protocol.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `device_model_protocol` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '모델-프로토콜 연결 ID',
  `model_id` int(11) NOT NULL COMMENT 'device_model.id (FK)',
  `protocol_type_id` int(11) NOT NULL COMMENT 'common_code.id (PROTOCOL_TYPE만)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_model_protocol_model_protocol` (`model_id`,`protocol_type_id`),
  KEY `idx_device_model_protocol_model_id` (`model_id`),
  KEY `idx_device_model_protocol_protocol_type_id` (`protocol_type_id`),
  CONSTRAINT `fk_device_model_protocol_model_id` FOREIGN KEY (`model_id`) REFERENCES `device_model` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_protocol_protocol_type_id` FOREIGN KEY (`protocol_type_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 모델별 지원 프로토콜 (N:M 연결)'
-- END 06_device_model_protocol.sql

-- BEGIN 07_device_model_snmp_point.sql
-- 07_device_model_snmp_point.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `device_model_snmp_point` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'SNMP point ID',
  `model_protocol_id` int(11) NOT NULL COMMENT 'device_model_protocol.id (FK)',
  `name` varchar(255) NOT NULL COMMENT '식별자·표시명 (V, 전압, PRI-FLOW 등)',
  `data_point_type_id` int(11) NOT NULL COMMENT 'common_code.id (code_group=DATA_POINT_TYPE)',
  `category_code_id` int(11) DEFAULT NULL COMMENT 'common_code.id (code_group=CATEGORY, 측정 항목 분류)',
  `unit_code_id` int(11) DEFAULT NULL COMMENT 'common_code.id (code_group=UNIT, 단위가 없으면 NULL)',
  `oid` varchar(512) NOT NULL COMMENT 'SNMP OID 또는 {instanceId} 템플릿',
  `requires_instance` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'instanceId 치환 필요 여부 (0=false, 1=true)',
  `scale` double DEFAULT NULL COMMENT '(NULL=1)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_model_snmp_point_protocol_name` (`model_protocol_id`,`name`),
  UNIQUE KEY `uk_device_model_snmp_point_protocol_oid` (`model_protocol_id`,`oid`),
  KEY `idx_device_model_snmp_point_model_protocol_id` (`model_protocol_id`),
  KEY `idx_device_model_snmp_point_data_point_type_id` (`data_point_type_id`),
  KEY `idx_device_model_snmp_point_category_code_id` (`category_code_id`),
  KEY `idx_device_model_snmp_point_unit_code_id` (`unit_code_id`),
  CONSTRAINT `fk_device_model_snmp_point_data_point_type_id` FOREIGN KEY (`data_point_type_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_snmp_point_category_code_id` FOREIGN KEY (`category_code_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_snmp_point_unit_code_id` FOREIGN KEY (`unit_code_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_snmp_point_model_protocol_id` FOREIGN KEY (`model_protocol_id`) REFERENCES `device_model_protocol` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 모델별 SNMP 수집 point (OID 카탈로그)'
-- END 07_device_model_snmp_point.sql

-- BEGIN 08_device_model_modbus_point.sql
-- 08_device_model_modbus_point.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `device_model_modbus_point` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Modbus point ID',
  `model_protocol_id` int(11) NOT NULL COMMENT 'device_model_protocol.id (FK)',
  `name` varchar(255) NOT NULL COMMENT '식별자·표시명 (TOTAL_WT, ONTO-TEMP 등)',
  `data_point_type_id` int(11) DEFAULT NULL COMMENT 'common_code.id (code_group=DATA_POINT_TYPE, 미분류 시 NULL)',
  `category_code_id` int(11) DEFAULT NULL COMMENT 'common_code.id (code_group=CATEGORY, 측정 항목 분류)',
  `unit_code_id` int(11) DEFAULT NULL COMMENT 'common_code.id (code_group=UNIT, 단위가 없으면 NULL)',
  `register_type` varchar(30) NOT NULL COMMENT '레지스터 종류 (COIL/DISCRETE/HOLDING/INPUT)',
  `data_type` varchar(20) NOT NULL COMMENT '값 해석 타입 (INT16/UINT16/INT32/UINT32/FLOAT32)',
  `byte_order` varchar(10) DEFAULT NULL COMMENT '멀티 레지스터 바이트 순서 (ABCD/CDAB/BADC/DCBA), 단일이면 NULL',
  `address` int(11) DEFAULT NULL COMMENT '레지스터 주소 (0~65535). requires_instance=1이면 NULL',
  `requires_instance` tinyint(1) NOT NULL DEFAULT 0 COMMENT '주소를 인스턴스가 제공하는지 (0=false, 1=true)',
  `scale` double DEFAULT NULL COMMENT '원시값에 곱할 배율 (NULL이면 1)',
  `value_offset` double DEFAULT NULL COMMENT '배율 적용 후 더할 값 (NULL이면 0)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_model_modbus_point_protocol_name` (`model_protocol_id`,`name`),
  KEY `idx_device_model_modbus_point_model_protocol_id` (`model_protocol_id`),
  KEY `idx_device_model_modbus_point_data_point_type_id` (`data_point_type_id`),
  KEY `idx_device_model_modbus_point_category_code_id` (`category_code_id`),
  KEY `idx_device_model_modbus_point_unit_code_id` (`unit_code_id`),
  CONSTRAINT `fk_device_model_modbus_point_model_protocol_id` FOREIGN KEY (`model_protocol_id`) REFERENCES `device_model_protocol` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_modbus_point_data_point_type_id` FOREIGN KEY (`data_point_type_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_modbus_point_category_code_id` FOREIGN KEY (`category_code_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_modbus_point_unit_code_id` FOREIGN KEY (`unit_code_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_device_model_modbus_point_requires_instance` CHECK (`requires_instance` in (0,1)),
  CONSTRAINT `chk_device_model_modbus_point_enabled` CHECK (`enabled` in (0,1)),
  CONSTRAINT `chk_device_model_modbus_point_address` CHECK (`requires_instance` = 1 and `address` is null or `requires_instance` = 0 and `address` is not null and `address` between 0 and 65535)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 모델별 Modbus 수집 point (레지스터 카탈로그)'
-- END 08_device_model_modbus_point.sql

-- BEGIN 09_devices.sql
-- 09_devices.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `devices` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '장비 ID (API {deviceId}, Influx tag device_id)',
  `model_id` int(11) NOT NULL COMMENT 'device_model.id (FK) — 제품 카탈로그',
  `location_node_code` char(10) NOT NULL COMMENT 'location_node.code (FK, 필수 — 미지정 시 UNASSIGNED)',
  `path_code_id` int(11) DEFAULT NULL COMMENT 'LOCATION_PATH common_code.id (PDU Path 피드)',
  `name` varchar(255) NOT NULL COMMENT '현장 표시명',
  `description` varchar(1000) DEFAULT NULL COMMENT '설명',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  `asset_code` varchar(100) DEFAULT NULL COMMENT '구버전 자산번호 (신규 자산 정보는 device_asset 사용)',
  `serial_number` varchar(200) DEFAULT NULL COMMENT '구버전 제조사 시리얼번호 (신규 자산 정보는 device_asset 사용)',
  `asset_status_id` int(11) DEFAULT NULL COMMENT '구버전 자산 운영 상태 코드 (신규 자산 정보는 device_asset 사용)',
  `asset_color` varchar(20) DEFAULT NULL COMMENT '구버전 자산 표시 색상 (신규 자산 정보는 device_asset 사용)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_devices_location_node_code_name` (`location_node_code`,`name`),
  UNIQUE KEY `uk_devices_asset_code` (`asset_code`),
  KEY `idx_devices_model_enabled` (`model_id`,`enabled`),
  KEY `idx_devices_location_enabled` (`location_node_code`,`enabled`),
  KEY `idx_devices_path_code_id` (`path_code_id`),
  KEY `idx_devices_asset_status_id` (`asset_status_id`),
  CONSTRAINT `fk_devices_asset_status_id` FOREIGN KEY (`asset_status_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_devices_location_node_code` FOREIGN KEY (`location_node_code`) REFERENCES `location_node` (`code`) ON UPDATE CASCADE,
  CONSTRAINT `fk_devices_model_id` FOREIGN KEY (`model_id`) REFERENCES `device_model` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_devices_path_code_id` FOREIGN KEY (`path_code_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `chk_devices_enabled` CHECK (`enabled` in (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 인스턴스 (현장 1대 = 1행, 얇은 인스턴스층)'
-- END 09_devices.sql

-- BEGIN 10_device_protocol_endpoint.sql
-- 10_device_protocol_endpoint.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `device_protocol_endpoint` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '엔드포인트 ID',
  `device_id` int(11) NOT NULL COMMENT 'devices.id (FK)',
  `protocol_type_id` int(11) NOT NULL COMMENT 'common_code.id (PROTOCOL_TYPE만)',
  `host` varchar(255) NOT NULL COMMENT 'IP 또는 hostname',
  `port` int(11) NOT NULL COMMENT '포트 (1~65535)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_protocol_endpoint_device_protocol` (`device_id`,`protocol_type_id`),
  UNIQUE KEY `uk_device_protocol_endpoint_host_port` (`host`,`port`),
  KEY `idx_device_protocol_endpoint_device_id` (`device_id`),
  KEY `idx_device_protocol_endpoint_protocol_type_id` (`protocol_type_id`),
  CONSTRAINT `fk_device_protocol_endpoint_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_protocol_endpoint_protocol_type_id` FOREIGN KEY (`protocol_type_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `chk_device_protocol_endpoint_enabled` CHECK (`enabled` in (0,1)),
  CONSTRAINT `chk_device_protocol_endpoint_port` CHECK (`port` >= 1 and `port` <= 65535)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 프로토콜 엔드포인트 (host/port 공통 전송층)'
-- END 10_device_protocol_endpoint.sql

-- BEGIN 11_device_snmp_instance.sql
-- 11_device_snmp_instance.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `device_snmp_instance` (
  `endpoint_id` int(11) NOT NULL COMMENT 'device_protocol_endpoint.id (PK/FK, SNMP endpoint 1:1)',
  `instance_id` int(11) NOT NULL COMMENT 'SNMP MIB instance index ({instanceId} 치환값)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`endpoint_id`),
  CONSTRAINT `fk_device_snmp_instance_endpoint_id` FOREIGN KEY (`endpoint_id`) REFERENCES `device_protocol_endpoint` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_device_snmp_instance_id` CHECK (`instance_id` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 SNMP instance 인덱스 (OID {instanceId} 치환, 필요한 endpoint만)'
-- END 11_device_snmp_instance.sql

-- BEGIN 12_collection_task.sql
-- 12_collection_task.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `collection_task` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Task ID',
  `name` varchar(100) NOT NULL COMMENT 'Task 이름',
  `model_id` int(11) NOT NULL COMMENT 'device_model.id',
  `script_type_id` int(11) NOT NULL COMMENT 'common_code.id (PROTOCOL_TYPE)',
  `active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '활성 여부',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '수집 Task 생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수집 Task 수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collection_task_model_script` (`model_id`,`script_type_id`),
  KEY `idx_collection_task_script_type_id` (`script_type_id`),
  KEY `idx_collection_task_active` (`active`),
  CONSTRAINT `fk_collection_task_model_id` FOREIGN KEY (`model_id`) REFERENCES `device_model` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_collection_task_script_type_id` FOREIGN KEY (`script_type_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `chk_collection_task_active` CHECK (`active` in (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='모델+프로토콜당 수집 Task 1개'
-- END 12_collection_task.sql

-- BEGIN 13_collection_task_group.sql
-- 13_collection_task_group.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `collection_task_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '그룹 ID',
  `task_id` int(11) NOT NULL COMMENT 'collection_task.id',
  `name` varchar(100) NOT NULL COMMENT '그룹 이름',
  `cron_expression` varchar(100) NOT NULL COMMENT '수집 주기 cron',
  `generated_spec` longtext DEFAULT NULL COMMENT '그룹 수집 JSON spec',
  `collector_job_id` varchar(100) DEFAULT NULL COMMENT 'collector job ID',
  `active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '그룹 활성 여부',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '주기 그룹 생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '주기 그룹 수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collection_task_group_task_cron` (`task_id`,`cron_expression`),
  UNIQUE KEY `uk_collection_task_group_collector_job_id` (`collector_job_id`),
  KEY `idx_collection_task_group_task_id` (`task_id`),
  CONSTRAINT `fk_collection_task_group_task_id` FOREIGN KEY (`task_id`) REFERENCES `collection_task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_collection_task_group_active` CHECK (`active` in (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Task 안 주기 그룹 (1분/5분 등)'
-- END 13_collection_task_group.sql

-- BEGIN 14_collection_task_device.sql
-- 14_collection_task_device.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `collection_task_device` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
  `group_id` int(11) NOT NULL COMMENT 'collection_task_group.id',
  `device_id` int(11) NOT NULL COMMENT 'devices.id',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT 'Task-장비 연결 생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT 'Task-장비 연결 수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collection_task_device_group_device` (`group_id`,`device_id`),
  KEY `idx_collection_task_device_device_id` (`device_id`),
  CONSTRAINT `fk_collection_task_device_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_collection_task_device_group_id` FOREIGN KEY (`group_id`) REFERENCES `collection_task_group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주기 그룹에 속한 장비. 한 Task 안에서 device는 그룹 1개만'
-- END 14_collection_task_device.sql

-- BEGIN 15_page_widget.sql
-- 15_page_widget.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '위젯 ID',
  `page_code_id` int(11) NOT NULL COMMENT 'common_code.id (DEVICE_PAGE)',
  `name` varchar(100) NOT NULL COMMENT '위젯 표시명',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
  `visual_style` varchar(16) NOT NULL DEFAULT 'classic' COMMENT '위젯 디자인',
  `visual_color` varchar(7) DEFAULT NULL COMMENT '위젯 강조 색상',
  `trend_range_preset` varchar(24) NOT NULL DEFAULT 'last_3d',
  `trend_window` varchar(8) NOT NULL DEFAULT '15m',
  `table_columns_json` text DEFAULT NULL COMMENT '테이블 열 설정',
  `data_freshness_minutes` int(11) NOT NULL DEFAULT 15 COMMENT '원천 데이터 최신 허용 경과 분',
  `query_kind` varchar(32) NOT NULL COMMENT 'last | aggregate | count | chart | calculated | psychrometric | power_distribution | cctv',
  `group_by` varchar(16) DEFAULT NULL COMMENT 'device | point | location',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_page_name` (`page_code_id`,`name`),
  KEY `idx_page_widget_page_code_id` (`page_code_id`),
  CONSTRAINT `fk_page_widget_page_code_id` FOREIGN KEY (`page_code_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `chk_page_widget_enabled` CHECK (`enabled` in (0,1)),
  CONSTRAINT `chk_page_widget_visual_style` CHECK (`visual_style` in ('classic','minimal','vivid')),
  CONSTRAINT `chk_page_widget_data_freshness` CHECK (`data_freshness_minutes` between 1 and 1440),
  CONSTRAINT `chk_page_widget_group_by` CHECK (`group_by` is null or `group_by` in ('device','point','location')),
  CONSTRAINT `chk_page_widget_query_kind` CHECK (`query_kind` in ('last','aggregate','count','chart','calculated','psychrometric','power_distribution','cctv'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='페이지 위젯 카드 정의 (DEVICE_PAGE 자식)'
-- END 15_page_widget.sql

-- BEGIN 16_page_widget_aggregate.sql
-- 16_page_widget_aggregate.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget_aggregate` (
  `widget_id` int(11) NOT NULL COMMENT '집계 위젯 ID',
  `op` varchar(16) NOT NULL COMMENT 'usage | power | usage_rank | power_rank | demand',
  `range_preset` varchar(16) DEFAULT NULL COMMENT 'last_24h | today | yesterday | last_7d | this_month | last_month',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '설정 생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '설정 수정 시각',
  PRIMARY KEY (`widget_id`),
  CONSTRAINT `fk_page_widget_aggregate_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_page_widget_aggregate_op` CHECK (`op` in ('usage','power','usage_rank','power_rank','demand')),
  CONSTRAINT `chk_page_widget_aggregate_range_preset` CHECK (`range_preset` is null or `range_preset` in ('last_24h','today','yesterday','last_7d','this_month','last_month'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용량 또는 전력 집계 위젯 설정'
-- END 16_page_widget_aggregate.sql

-- BEGIN 17_page_widget_count.sql
-- 17_page_widget_count.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget_count` (
  `widget_id` int(11) NOT NULL COMMENT '장비 수 위젯 ID',
  `count_mode` varchar(16) NOT NULL COMMENT '장비 수 집계 방식',
  `count_model_id` int(11) DEFAULT NULL COMMENT '모델별 집계 시 대상 모델 ID',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '설정 생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '설정 수정 시각',
  PRIMARY KEY (`widget_id`),
  KEY `idx_pwc_count_model_id` (`count_model_id`),
  CONSTRAINT `fk_pwc_model_id` FOREIGN KEY (`count_model_id`) REFERENCES `device_model` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_pwc_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_pwc_count_mode` CHECK (`count_mode` in ('total','by_model','model'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 수 위젯 집계 설정'
-- END 17_page_widget_count.sql

-- BEGIN 18_page_widget_chart.sql
-- 18_page_widget_chart.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget_chart` (
  `widget_id` int(11) NOT NULL COMMENT '차트 위젯 ID',
  `chart_scope` varchar(16) NOT NULL COMMENT '장비 또는 모델 기준 차트 범위',
  `chart_series_mode` varchar(16) NOT NULL COMMENT '시계열 표시·집계 방식',
  `chart_range_preset` varchar(16) NOT NULL COMMENT '차트 기본 조회 기간',
  `chart_window` varchar(8) NOT NULL COMMENT '차트 시간 창 설정',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '설정 생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '설정 수정 시각',
  PRIMARY KEY (`widget_id`),
  CONSTRAINT `fk_pwch_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_pwch_scope` CHECK (`chart_scope` in ('devices','models')),
  CONSTRAINT `chk_pwch_series_mode` CHECK (`chart_series_mode` in ('per_device','sum','by_phase','by_path')),
  CONSTRAINT `chk_pwch_range_preset` CHECK (`chart_range_preset` in ('last_24h','today','yesterday','last_3d','last_7d','this_month','last_month'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='측정 추이 차트 위젯 설정'
-- END 18_page_widget_chart.sql

-- BEGIN 19_page_widget_point.sql
-- 19_page_widget_point.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget_point` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `point_name` varchar(100) NOT NULL COMMENT 'Influx point_name',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_point_widget_name` (`widget_id`,`point_name`),
  KEY `idx_page_widget_point_widget_id` (`widget_id`),
  CONSTRAINT `fk_page_widget_point_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='페이지 위젯 조회 포인트 (1:N)'
-- END 19_page_widget_point.sql

-- BEGIN 20_page_widget_device.sql
-- 20_page_widget_device.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget_device` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `device_id` int(11) NOT NULL COMMENT 'devices.id',
  `device_role` varchar(16) DEFAULT NULL COMMENT 'NULL|default',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_device_widget_device` (`widget_id`,`device_id`),
  KEY `idx_page_widget_device_widget_id` (`widget_id`),
  KEY `idx_page_widget_device_device_id` (`device_id`),
  CONSTRAINT `fk_page_widget_device_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_page_widget_device_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_page_widget_device_role` CHECK (`device_role` is null or `device_role` in ('default','total','it'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='페이지 위젯 조회 장비 (1:N)'
-- END 20_page_widget_device.sql

-- BEGIN 21_page_widget_model.sql
-- 21_page_widget_model.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget_model` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '위젯 모델 연결 ID',
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `model_id` int(11) NOT NULL COMMENT 'device_model.id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_model_widget_model` (`widget_id`,`model_id`),
  KEY `idx_page_widget_model_widget_id` (`widget_id`),
  KEY `idx_page_widget_model_model_id` (`model_id`),
  CONSTRAINT `fk_page_widget_model_model_id` FOREIGN KEY (`model_id`) REFERENCES `device_model` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_page_widget_model_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='chart widget model scope (1:N)'
-- END 21_page_widget_model.sql

-- BEGIN 22_page_widget_layout.sql
-- 22_page_widget_layout.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget_layout` (
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id (PK, 1:1)',
  `grid_x` int(11) NOT NULL COMMENT '그리드 X',
  `grid_y` int(11) NOT NULL COMMENT '그리드 Y',
  `w` int(11) NOT NULL COMMENT '가로 칸 수',
  `h` int(11) NOT NULL COMMENT '세로 칸 수',
  `grid_version` int(11) NOT NULL DEFAULT 48 COMMENT '그리드 기준 버전 (48=48열 기준, 0=레거시 자동 판별)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`widget_id`),
  CONSTRAINT `fk_page_widget_layout_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_page_widget_layout_grid_x` CHECK (`grid_x` >= 0),
  CONSTRAINT `chk_page_widget_layout_grid_y` CHECK (`grid_y` >= 0),
  CONSTRAINT `chk_page_widget_layout_w` CHECK (`w` >= 1),
  CONSTRAINT `chk_page_widget_layout_h` CHECK (`h` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='페이지 위젯 UI 그리드 배치 2D (1:1)'
-- END 22_page_widget_layout.sql

-- BEGIN 23_device_endpoint_modbus.sql
-- 23_device_endpoint_modbus.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `device_endpoint_modbus` (
  `endpoint_id` int(11) NOT NULL COMMENT 'device_protocol_endpoint.id (PK/FK, Modbus endpoint 1:1)',
  `unit_id` int(11) DEFAULT NULL COMMENT 'Modbus unit/slave ID (0~247). 회선별로 다르면 NULL (매핑 테이블에서 지정)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`endpoint_id`),
  CONSTRAINT `fk_device_endpoint_modbus_endpoint_id` FOREIGN KEY (`endpoint_id`) REFERENCES `device_protocol_endpoint` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_device_endpoint_modbus_unit_id` CHECK (`unit_id` is null or `unit_id` >= 0 and `unit_id` <= 247)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 Modbus 엔드포인트 확장 (unit_id, 필요한 endpoint만)'
-- END 23_device_endpoint_modbus.sql

-- BEGIN 24_calculated_metric.sql
CREATE TABLE IF NOT EXISTS `calculated_metric` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '계산 지표 고유 ID',
  `name` varchar(100) NOT NULL COMMENT '계산 지표 이름',
  `calculation_cron` varchar(64) NOT NULL DEFAULT '0 */5 * * * *' COMMENT '계산 실행 주기 (6필드 cron)',
  `collection_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '계산 지표 수집 활성 여부',
  `config_version` int(11) NOT NULL DEFAULT 1 COMMENT 'Collector 설정 동기화 버전',
  `collector_job_id` varchar(100) DEFAULT NULL COMMENT 'Collector에 등록된 계산 작업 ID',
  `formula` varchar(500) NOT NULL COMMENT 'alias를 사용하는 계산식',
  `result_unit` varchar(32) DEFAULT NULL COMMENT '계산 결과 단위',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_calculated_metric_name` (`name`),
  CONSTRAINT `chk_calculated_metric_enabled` CHECK (`collection_enabled` in (0,1)),
  CONSTRAINT `chk_calculated_metric_version` CHECK (`config_version` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Collector에서 주기적으로 계산하는 지표 정의';
-- END 24_calculated_metric.sql

-- BEGIN 25_calculated_metric_source.sql
CREATE TABLE IF NOT EXISTS `calculated_metric_source` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '계산 지표 입력 소스 ID',
  `calculated_metric_id` int(11) NOT NULL COMMENT '입력 소스가 속한 계산 지표 ID',
  `device_id` int(11) NOT NULL COMMENT '입력값을 제공하는 장비 ID',
  `alias` varchar(32) NOT NULL COMMENT '계산식에서 참조할 변수명',
  `protocol` varchar(16) NOT NULL COMMENT '원본 측정값 수집 프로토콜',
  `point_name` varchar(100) NOT NULL COMMENT '원본 장비 측정 포인트 이름',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_calculated_metric_source_alias` (`calculated_metric_id`,`alias`),
  KEY `idx_calculated_metric_source_device` (`device_id`),
  CONSTRAINT `fk_calculated_metric_source_metric` FOREIGN KEY (`calculated_metric_id`) REFERENCES `calculated_metric` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_calculated_metric_source_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`),
  CONSTRAINT `chk_calculated_metric_source_protocol` CHECK (`protocol` in ('snmp','modbus'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='계산 지표 alias와 장비 측정 포인트의 연결';
-- END 25_calculated_metric_source.sql

-- BEGIN 26_page_widget_calculated.sql
CREATE TABLE IF NOT EXISTS `page_widget_calculated` (
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `calculated_metric_id` int(11) NOT NULL COMMENT '계산 지표 정의',
  `range_preset` varchar(32) NOT NULL DEFAULT 'last_24h' COMMENT '위젯에 표시할 계산 결과 기간',
  `freshness_minutes` int(11) NOT NULL DEFAULT 15 COMMENT '마지막 계산 결과 허용 경과 분',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`widget_id`),
  KEY `idx_page_widget_calculated_metric` (`calculated_metric_id`),
  CONSTRAINT `fk_page_widget_calculated_widget` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_page_widget_calculated_metric` FOREIGN KEY (`calculated_metric_id`) REFERENCES `calculated_metric` (`id`),
  CONSTRAINT `chk_page_widget_calculated_range` CHECK (`range_preset` in ('last_24h','today','yesterday','last_7d','this_month','last_month')),
  CONSTRAINT `chk_page_widget_calculated_freshness` CHECK (`freshness_minutes` between 1 and 1440)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='계산 지표 위젯의 표시 기간 및 신선도 설정';
-- END 26_page_widget_calculated.sql

-- BEGIN 27_page_widget_psychrometric.sql
CREATE TABLE page_widget_psychrometric (
    widget_id INT NOT NULL COMMENT 'page_widget.id 및 심리도 위젯 ID',
    PRIMARY KEY (widget_id),
    CONSTRAINT fk_page_widget_psychrometric_widget
        FOREIGN KEY (widget_id) REFERENCES page_widget (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='심리도 위젯의 1:1 설정';
-- END 27_page_widget_psychrometric.sql

-- BEGIN 28_page_widget_psychrometric_source.sql
CREATE TABLE page_widget_psychrometric_source (
    id INT NOT NULL AUTO_INCREMENT COMMENT '심리도 측정 소스 ID',
    widget_id INT NOT NULL COMMENT '심리도 위젯 ID',
    device_id INT NOT NULL COMMENT '측정 소스 장비 ID',
    role VARCHAR(16) NOT NULL COMMENT '심리도 입력 역할 (예: 온도/습도)',
    point_name VARCHAR(100) NOT NULL COMMENT '장비에서 조회할 측정 포인트 이름',
    created_dt DATETIME(6) NULL COMMENT '소스 등록 시각',
    updated_dt DATETIME(6) NULL COMMENT '소스 수정 시각',
    PRIMARY KEY (id),
    CONSTRAINT uk_page_widget_psych_source UNIQUE (widget_id, device_id, point_name),
    CONSTRAINT fk_page_widget_psych_source_widget
        FOREIGN KEY (widget_id) REFERENCES page_widget_psychrometric (widget_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_page_widget_psych_source_device
        FOREIGN KEY (device_id) REFERENCES devices (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='심리도 위젯별 장비 측정 소스';
-- END 28_page_widget_psychrometric_source.sql

-- BEGIN 29_page_widget_last_source.sql
CREATE TABLE IF NOT EXISTS `page_widget_last_source` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '최신값 위젯 장비별 측정항목 ID',
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `device_id` int(11) NOT NULL COMMENT 'devices.id',
  `point_name` varchar(100) NOT NULL COMMENT '해당 장비에서 조회할 측정항목명',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '측정 소스 생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '측정 소스 수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_last_source_widget_device_point` (`widget_id`,`device_id`,`point_name`),
  KEY `idx_page_widget_last_source_widget_id` (`widget_id`),
  KEY `idx_page_widget_last_source_device_id` (`device_id`),
  CONSTRAINT `fk_page_widget_last_source_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_page_widget_last_source_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='latest 위젯 장비별 측정항목 매핑';
-- END 29_page_widget_last_source.sql

-- BEGIN 30_page_widget_power_distribution.sql
CREATE TABLE IF NOT EXISTS `page_widget_power_distribution` (
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`widget_id`),
  CONSTRAINT `fk_page_widget_power_distribution_widget`
    FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='전력 분배 위젯 정의';
-- END 30_page_widget_power_distribution.sql

-- BEGIN 31_page_widget_power_distribution_group.sql
CREATE TABLE IF NOT EXISTS `page_widget_power_distribution_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '그룹 ID',
  `widget_id` int(11) NOT NULL COMMENT 'page_widget_power_distribution.widget_id',
  `name` varchar(100) NOT NULL COMMENT '그룹 표시명',
  `color` varchar(16) DEFAULT NULL COMMENT '표시 색상 #RRGGBB',
  `sort_order` int(11) NOT NULL COMMENT '표시 순서',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_power_distribution_group_name` (`widget_id`, `name`),
  KEY `idx_page_widget_power_distribution_group_widget` (`widget_id`),
  CONSTRAINT `fk_page_widget_power_distribution_group_widget`
    FOREIGN KEY (`widget_id`) REFERENCES `page_widget_power_distribution` (`widget_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='전력 분배 위젯 그룹';
-- END 31_page_widget_power_distribution_group.sql

-- BEGIN 32_page_widget_power_distribution_source.sql
CREATE TABLE IF NOT EXISTS `page_widget_power_distribution_source` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '소스 ID',
  `group_id` int(11) NOT NULL COMMENT 'page_widget_power_distribution_group.id',
  `device_id` int(11) NOT NULL COMMENT 'devices.id',
  `point_name` varchar(100) NOT NULL COMMENT 'W 단위 POWER 포인트명',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_power_distribution_source` (`group_id`, `device_id`, `point_name`),
  KEY `idx_page_widget_power_distribution_source_group` (`group_id`),
  KEY `idx_page_widget_power_distribution_source_device` (`device_id`),
  CONSTRAINT `fk_page_widget_power_distribution_source_group`
    FOREIGN KEY (`group_id`) REFERENCES `page_widget_power_distribution_group` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_page_widget_power_distribution_source_device`
    FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='전력 분배 위젯 그룹별 POWER 소스';
-- END 32_page_widget_power_distribution_source.sql

-- BEGIN 33_device_group.sql
-- 33_device_group.sql — 물리 위치와 별개로 사용하는 범용 장비 그룹

CREATE TABLE IF NOT EXISTS `device_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '장비 그룹 ID',
  `name` varchar(100) NOT NULL COMMENT '그룹명',
  `description` varchar(1000) DEFAULT NULL COMMENT '설명',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_group_name` (`name`),
  CONSTRAINT `chk_device_group_enabled` CHECK (`enabled` in (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='범용 장비 그룹 (IT, Cooling, A계통 등)';
-- END 33_device_group.sql

-- BEGIN 34_device_group_device.sql
-- 34_device_group_device.sql — 장비 그룹과 장비의 다대다 연결

CREATE TABLE IF NOT EXISTS `device_group_device` (
  `device_group_id` int(11) NOT NULL COMMENT 'device_group.id',
  `device_id` int(11) NOT NULL COMMENT 'devices.id',
  PRIMARY KEY (`device_group_id`, `device_id`),
  KEY `idx_device_group_device_device` (`device_id`),
  CONSTRAINT `fk_device_group_device_group`
    FOREIGN KEY (`device_group_id`) REFERENCES `device_group` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_device_group_device_device`
    FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 그룹별 장비 연결';
-- END 34_device_group_device.sql

-- BEGIN 35_page_widget_device_group.sql
-- 35_page_widget_device_group.sql — 차트·집계 위젯의 동적 장비 그룹 대상

CREATE TABLE IF NOT EXISTS `page_widget_device_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '위젯 장비 그룹 연결 ID',
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `device_group_id` int(11) NOT NULL COMMENT 'device_group.id',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_device_group` (`widget_id`, `device_group_id`),
  KEY `idx_page_widget_device_group_group` (`device_group_id`),
  CONSTRAINT `fk_page_widget_device_group_widget`
    FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_page_widget_device_group_group`
    FOREIGN KEY (`device_group_id`) REFERENCES `device_group` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='위젯에서 참조하는 장비 그룹';
-- END 35_page_widget_device_group.sql

-- BEGIN 36_device_rack_placement.sql
CREATE TABLE IF NOT EXISTS `device_rack_placement` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Rack 배치 레코드 ID',
  `device_id` int(11) NOT NULL COMMENT '배치된 장비 ID',
  `mount_type` varchar(30) NOT NULL DEFAULT 'RACK_U' COMMENT 'RACK_U/RACK_SIDE/RACK_REAR/FLOOR/WALL',
  `rack_location_code` char(10) DEFAULT NULL COMMENT '장비를 배치한 Rack 위치 코드',
  `rack_side` varchar(20) DEFAULT NULL COMMENT 'LEFT/RIGHT/REAR',
  `u_position` int(11) DEFAULT NULL COMMENT 'RACK_U일 때만 사용',
  `u_height` int(11) DEFAULT NULL COMMENT 'RACK_U일 때만 사용',
  `form_factor` varchar(30) DEFAULT NULL COMMENT '장비 폼팩터 정보',
  `position_x` double DEFAULT NULL COMMENT '랙 내부 가로 상대 위치 (0~1)',
  `position_y` double DEFAULT NULL COMMENT '랙 내부 높이 상대 위치 (0~1)',
  `position_z` double DEFAULT NULL COMMENT '랙 내부 앞뒤 상대 위치 (0~1)',
  `visual_length_ratio` double DEFAULT NULL COMMENT '측면 장착 3D 모델의 랙 높이 대비 길이',
  `scene_model_key` varchar(32) DEFAULT NULL COMMENT '장착 장비의 3D 모델 키',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '배치 등록 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '배치 수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_rack_placement_device` (`device_id`),
  KEY `idx_device_rack_placement_rack_u` (`rack_location_code`,`u_position`),
  CONSTRAINT `fk_device_rack_placement_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_device_rack_placement_location` FOREIGN KEY (`rack_location_code`) REFERENCES `location_node` (`code`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_device_rack_placement_u_position` CHECK (`u_position` IS NULL OR `u_position` >= 1),
  CONSTRAINT `chk_device_rack_placement_u_height` CHECK (`u_height` IS NULL OR `u_height` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 Rack U 배치';
-- END 36_device_rack_placement.sql

-- BEGIN 37_device_image.sql
CREATE TABLE IF NOT EXISTS `device_image` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '장비 이미지 ID',
  `device_id` int(11) NOT NULL COMMENT '이미지가 연결된 장비 ID',
  `storage_key` varchar(255) NOT NULL COMMENT '서버 파일 저장 키',
  `original_name` varchar(255) NOT NULL COMMENT '업로드 원본 파일명',
  `content_type` varchar(100) NOT NULL COMMENT '이미지 MIME type',
  `file_size` bigint(20) NOT NULL COMMENT '파일 크기 (byte)',
  `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '이미지 표시 순서',
  `is_primary` tinyint(1) NOT NULL DEFAULT 0 COMMENT '대표 이미지 여부',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '등록 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_image_storage_key` (`storage_key`),
  KEY `idx_device_image_device_sort` (`device_id`,`is_primary`,`sort_order`),
  CONSTRAINT `fk_device_image_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_device_image_primary` CHECK (`is_primary` in (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 자산 이미지';
-- END 37_device_image.sql

-- BEGIN 38_device_rack_placement_history.sql
CREATE TABLE IF NOT EXISTS `device_rack_placement_history` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Rack 배치 이력 ID',
  `device_id` int(11) NOT NULL COMMENT '이력이 기록된 장비 ID',
  `action` varchar(20) NOT NULL COMMENT 'PLACED/MOVED/REMOVED',
  `previous_mount_type` varchar(30) DEFAULT NULL COMMENT '변경 전 장착 방식',
  `previous_rack_location_code` char(10) DEFAULT NULL COMMENT '변경 전 Rack 위치 코드',
  `previous_rack_side` varchar(20) DEFAULT NULL COMMENT '변경 전 Rack 방향',
  `previous_u_position` int(11) DEFAULT NULL COMMENT '변경 전 Rack U 시작 위치',
  `previous_u_height` int(11) DEFAULT NULL COMMENT '변경 전 점유 U 높이',
  `current_mount_type` varchar(30) DEFAULT NULL COMMENT '변경 후 장착 방식',
  `current_rack_location_code` char(10) DEFAULT NULL COMMENT '변경 후 Rack 위치 코드',
  `current_rack_side` varchar(20) DEFAULT NULL COMMENT '변경 후 Rack 방향',
  `current_u_position` int(11) DEFAULT NULL COMMENT '변경 후 Rack U 시작 위치',
  `current_u_height` int(11) DEFAULT NULL COMMENT '변경 후 점유 U 높이',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '이력 생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '이력 수정 시각',
  PRIMARY KEY (`id`),
  KEY `idx_device_rack_placement_history_device_dt` (`device_id`, `created_dt`),
  CONSTRAINT `fk_device_rack_placement_history_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 Rack 배치 변경 이력';
-- END 38_device_rack_placement_history.sql

-- BEGIN 39_device_asset_history.sql
CREATE TABLE IF NOT EXISTS `device_asset_history` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '자산 변경 이력 ID',
  `device_id` int(11) NOT NULL COMMENT '변경 대상 장비 ID',
  `action` varchar(30) NOT NULL COMMENT 'ASSET_UPDATED/STATUS_CHANGED/DOCUMENT_UPLOADED/DOCUMENT_DELETED',
  `actor_name` varchar(100) DEFAULT NULL COMMENT '변경 사용자 또는 시스템',
  `reason` varchar(1000) DEFAULT NULL COMMENT '상태 변경 사유',
  `previous_asset_code` varchar(100) DEFAULT NULL COMMENT '변경 전 자산번호',
  `current_asset_code` varchar(100) DEFAULT NULL COMMENT '변경 후 자산번호',
  `previous_serial_number` varchar(200) DEFAULT NULL COMMENT '변경 전 제조사 시리얼번호',
  `current_serial_number` varchar(200) DEFAULT NULL COMMENT '변경 후 제조사 시리얼번호',
  `previous_status_code` varchar(100) DEFAULT NULL COMMENT '변경 전 자산 상태 코드',
  `previous_status_name` varchar(255) DEFAULT NULL COMMENT '변경 전 자산 상태 표시명',
  `current_status_code` varchar(100) DEFAULT NULL COMMENT '변경 후 자산 상태 코드',
  `current_status_name` varchar(255) DEFAULT NULL COMMENT '변경 후 자산 상태 표시명',
  `previous_enabled` tinyint(1) DEFAULT NULL COMMENT '변경 전 장비 사용 여부',
  `current_enabled` tinyint(1) DEFAULT NULL COMMENT '변경 후 장비 사용 여부',
  `previous_installed_date` date DEFAULT NULL COMMENT '변경 전 설치일',
  `current_installed_date` date DEFAULT NULL COMMENT '변경 후 설치일',
  `previous_asset_manager_name` varchar(100) DEFAULT NULL COMMENT '변경 전 자산 담당자',
  `current_asset_manager_name` varchar(100) DEFAULT NULL COMMENT '변경 후 자산 담당자',
  `previous_supplier_name` varchar(255) DEFAULT NULL COMMENT '변경 전 공급업체',
  `current_supplier_name` varchar(255) DEFAULT NULL COMMENT '변경 후 공급업체',
  `previous_warranty_expires_on` date DEFAULT NULL COMMENT '변경 전 보증 만료일',
  `current_warranty_expires_on` date DEFAULT NULL COMMENT '변경 후 보증 만료일',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '이력 생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '이력 수정 시각',
  PRIMARY KEY (`id`),
  KEY `idx_device_asset_history_device_dt` (`device_id`, `created_dt`),
  CONSTRAINT `fk_device_asset_history_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 자산 정보 및 운영 상태 변경 이력';
-- END 39_device_asset_history.sql

-- BEGIN 40_device_asset.sql
-- 장비 인스턴스와 분리된 자산 관리 속성(1 장비 : 1 자산 상세)
-- 기존 운영 DB 이관 시에는 devices의 legacy 자산 컬럼을 이 테이블로 복사한 뒤 애플리케이션을 재기동한다.

CREATE TABLE IF NOT EXISTS `device_asset` (
  `device_id` int(11) NOT NULL COMMENT 'devices.id (PK/FK)',
  `asset_code` varchar(100) DEFAULT NULL COMMENT '내부 자산번호',
  `serial_number` varchar(200) DEFAULT NULL COMMENT '제조사 시리얼번호',
  `asset_status_id` int(11) DEFAULT NULL COMMENT 'ASSET_STATUS common_code.id',
  `asset_color` varchar(20) DEFAULT NULL COMMENT '자산 표시 색상',
  `installed_date` date DEFAULT NULL COMMENT '설치일',
  `asset_manager_name` varchar(100) DEFAULT NULL COMMENT '자산 담당자',
  `supplier_name` varchar(255) DEFAULT NULL COMMENT '공급사',
  `warranty_expires_on` date DEFAULT NULL COMMENT '보증 만료일',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`device_id`),
  UNIQUE KEY `uk_device_asset_asset_code` (`asset_code`),
  KEY `idx_device_asset_status_id` (`asset_status_id`),
  CONSTRAINT `fk_device_asset_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_asset_status` FOREIGN KEY (`asset_status_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 자산 관리 속성';
-- END 40_device_asset.sql

-- BEGIN 41_device_asset_document.sql
-- 자산별 첨부 문서 (자산당 여러 파일을 형식 구분 없이 관리)
CREATE TABLE IF NOT EXISTS `device_asset_document` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '자산 문서 ID',
  `device_id` int(11) NOT NULL COMMENT 'devices.id',
  `storage_key` varchar(255) NOT NULL COMMENT '서버 파일 저장 키',
  `original_name` varchar(255) NOT NULL COMMENT '업로드 원본 파일명',
  `content_type` varchar(150) NOT NULL COMMENT 'MIME type',
  `file_size` bigint(20) NOT NULL COMMENT '파일 크기(byte)',
  `file_kind` varchar(16) NOT NULL DEFAULT 'DOCUMENT' COMMENT '첨부 유형 (DOCUMENT/ASSET_2D/ASSET_3D)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '업로드 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_asset_document_storage_key` (`storage_key`),
  KEY `idx_device_asset_document_device_created` (`device_id`,`created_dt`),
  CONSTRAINT `fk_device_asset_document_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 자산 첨부 문서';
-- END 41_device_asset_document.sql

-- BEGIN 42_device_modbus_reading.sql
CREATE TABLE IF NOT EXISTS `device_modbus_reading` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
    `endpoint_id` int(11) NOT NULL COMMENT 'device_endpoint_modbus.endpoint_id — 수집 원본 Modbus 설정',
    `point_id` int(11) NOT NULL COMMENT 'device_model_modbus_point.id — 자료형·레지스터 유형·배율 등 모델 공통 수집 설정',
    `unit_id` int(11) NOT NULL COMMENT '이 회선의 Modbus unit/slave ID (0~247)',
    `address` int(11) NOT NULL COMMENT '보정이 끝난 실제 Modbus 요청 시작 주소 (0~65535)',
    `target_device_id` int(11) NOT NULL COMMENT 'devices.id — 결과를 기록할 장비 (Influx device_id 태그)',
    `point_name` varchar(255) NOT NULL COMMENT '결과를 저장할 필드명 (TOTAL_WT, POWER_A 등)',
    `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
    `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
    `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_modbus_reading_target_point_name` (`target_device_id`, `point_name`),
    KEY `idx_device_modbus_reading_endpoint_id` (`endpoint_id`),
    KEY `idx_device_modbus_reading_point_id` (`point_id`),
    KEY `idx_device_modbus_reading_target_device_id` (`target_device_id`),
    CONSTRAINT `fk_device_modbus_reading_modbus_endpoint_id` FOREIGN KEY (`endpoint_id`) REFERENCES `device_endpoint_modbus` (`endpoint_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_device_modbus_reading_point_id` FOREIGN KEY (`point_id`) REFERENCES `device_model_modbus_point` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_device_modbus_reading_target_device_id` FOREIGN KEY (`target_device_id`) REFERENCES `devices` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `chk_device_modbus_reading_unit_id` CHECK (`unit_id` BETWEEN 0 AND 247),
    CONSTRAINT `chk_device_modbus_reading_address` CHECK (`address` BETWEEN 0 AND 65535),
    CONSTRAINT `chk_device_modbus_reading_enabled` CHECK (`enabled` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Modbus 회선별 수집 주소 및 결과 목적지 매핑';
-- END 42_device_modbus_reading.sql

-- BEGIN 48_device_modbus_bit_field.sql
CREATE TABLE IF NOT EXISTS `device_modbus_bit_field` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Modbus 파생 비트 포인트 ID',
    `reading_id` int(11) NOT NULL COMMENT '원본 Modbus reading',
    `point_name` varchar(255) NOT NULL COMMENT '추출 결과 point 이름',
    `bit_offset` int(11) NOT NULL COMMENT 'LSB 기준 시작 비트 (0부터)',
    `bit_width` int(11) NOT NULL COMMENT '추출 비트 수 (1~32)',
    `value_map` text NOT NULL COMMENT '원시 비트값에서 저장값으로 변환하는 JSON 맵',
    `unmapped_value` bigint DEFAULT NULL COMMENT '매핑되지 않은 값의 대체값; NULL이면 원시 추출값 저장',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_modbus_bit_field_reading_name` (`reading_id`, `point_name`),
    CONSTRAINT `fk_modbus_bit_field_reading` FOREIGN KEY (`reading_id`)
        REFERENCES `device_modbus_reading` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_modbus_bit_field_offset` CHECK (`bit_offset` BETWEEN 0 AND 31),
    CONSTRAINT `chk_modbus_bit_field_width` CHECK (`bit_width` BETWEEN 1 AND 32),
    CONSTRAINT `chk_modbus_bit_field_range` CHECK (`bit_offset` + `bit_width` <= 32)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Modbus reading 원본 레지스터의 비트 구간별 파생 point';
-- END 48_device_modbus_bit_field.sql

-- BEGIN 49_device_page_device.sql
-- DEVICE_PAGE(common_code)별 표시 장비. 위젯 소스와 독립적인 페이지 장비 선택.
CREATE TABLE IF NOT EXISTS `device_page_device` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '페이지 장비 연결 ID',
    `page_code_id` int(11) NOT NULL COMMENT 'common_code.id (DEVICE_PAGE)',
    `device_id` int(11) NOT NULL COMMENT '페이지에 표시할 장비 ID',
    `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '연결 생성 시각',
    `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '연결 수정 시각',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_page_device_page_device` (`page_code_id`, `device_id`),
    KEY `idx_device_page_device_device_id` (`device_id`),
    CONSTRAINT `fk_device_page_device_page_code` FOREIGN KEY (`page_code_id`)
        REFERENCES `common_code` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_device_page_device_device` FOREIGN KEY (`device_id`)
        REFERENCES `devices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='DEVICE_PAGE에 직접 선택한 장비';
-- END 49_device_page_device.sql

-- BEGIN 50_device_page_model_point_setting.sql
-- 페이지·장비 모델별 측정 포인트의 노출 여부와 표시 순서를 관리합니다.
-- point_id는 프로토콜별 모델 포인트 테이블 중 하나를 가리키므로,
-- 해당 포인트가 지정된 모델과 프로토콜에 속하는지는 애플리케이션 서비스에서 검증합니다.
CREATE TABLE IF NOT EXISTS `device_page_model_point_setting` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '페이지 포인트 설정 ID',
    `page_code_id` int(11) NOT NULL COMMENT 'common_code.id — DEVICE_PAGE 그룹의 페이지 코드',
    `model_id` int(11) NOT NULL COMMENT 'device_model.id — 같은 모델의 모든 장비 인스턴스가 공유하는 설정',
    `protocol_type_id` int(11) NOT NULL COMMENT 'common_code.id — PROTOCOL_TYPE 그룹의 프로토콜 코드',
    `point_id` int(11) NOT NULL COMMENT '프로토콜별 모델 포인트 ID — 모델·프로토콜 소속 여부는 애플리케이션에서 검증',
    `is_visible` tinyint(1) NOT NULL DEFAULT 0 COMMENT '해당 페이지에 포인트를 표시할지 여부 (0=false, 1=true)',
    `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '페이지·모델 내 포인트 표시 순서',
    `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
    `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_page_model_protocol_point`
        (`page_code_id`, `model_id`, `protocol_type_id`, `point_id`),
    KEY `idx_page_model_point_setting_model` (`model_id`),
    KEY `idx_page_model_point_setting_protocol` (`protocol_type_id`),
    CONSTRAINT `fk_page_model_point_setting_page`
        FOREIGN KEY (`page_code_id`) REFERENCES `common_code` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_page_model_point_setting_model`
        FOREIGN KEY (`model_id`) REFERENCES `device_model` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_page_model_point_setting_protocol`
        FOREIGN KEY (`protocol_type_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_page_model_point_setting_visible` CHECK (`is_visible` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='페이지별 장비 모델 측정 포인트 노출 및 표시 순서 설정';
-- END 50_device_page_model_point_setting.sql

-- BEGIN 52_external_data.sql
-- 외부 시스템에서 수신한 범용 업무 데이터를 원본 이력으로 저장한다.
-- 업무 기준 날짜/에너지 종류/측정값은 payload_json 내부에 보관한다.
CREATE TABLE IF NOT EXISTS `external_data` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '외부 데이터 수신 레코드 ID',
    `data_category` varchar(50) NOT NULL COMMENT '데이터 분류 (예: BILLING, CONSUMPTION, RATE)',
    `period_type` varchar(30) NOT NULL COMMENT '자료 주기 (예: MONTHLY, DAILY, REALTIME)',
    `payload_json` LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '외부 수신 원본 JSON payload' CHECK (json_valid(`payload_json`)),
    `create_dt` timestamp(6) NOT NULL DEFAULT current_timestamp(6) COMMENT 'Manager가 데이터를 수신해 저장한 시각',
    PRIMARY KEY (`id`),
    KEY `idx_external_data_category_period_created` (`data_category`, `period_type`, `create_dt`, `id`),
  KEY `idx_external_data_created` (`create_dt`, `id`),
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='외부 데이터 JSON 수신 이력';
-- END 52_external_data.sql

-- BEGIN 53_device_mqtt_identity.sql
-- 신규 DB용 최종 스키마. 기존 LoRa DB는 sql/migration/20261014_unify_lora_mqtt.sql을 적용한다.
CREATE TABLE IF NOT EXISTS `device_mqtt_identity` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'MQTT 식별자 매핑 ID',
  `device_id` int(11) NOT NULL COMMENT 'devices.id (FK, Influx device_id)',
  `identity_type` varchar(20) NOT NULL COMMENT 'MAC, DEV_EUI, DEVICE_NAME, UUID, SERIAL',
  `external_id` varchar(255) NOT NULL COMMENT '현재 외부 식별값 원본',
  `normalized_external_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '유형별 정규화 식별값, 수신 매칭 기준',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '매칭 사용 여부',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '식별값 연결 생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '식별값 연결 수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_mqtt_identity_type_normalized` (`identity_type`,`normalized_external_id`),
  UNIQUE KEY `uk_device_mqtt_identity_device_type` (`device_id`,`identity_type`),
  KEY `idx_device_mqtt_identity_device_id` (`device_id`),
  CONSTRAINT `fk_device_mqtt_identity_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_device_mqtt_identity_type` CHECK (`identity_type` IN ('MAC','DEV_EUI','DEVICE_NAME','UUID','SERIAL')),
  CONSTRAINT `chk_device_mqtt_identity_enabled` CHECK (`enabled` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MQTT 외부 식별값과 내부 장비의 현재 연결 (LoRa 포함)';
-- END 53_device_mqtt_identity.sql

-- BEGIN 54_device_model_mqtt_point.sql
-- DEVICE: payload_field는 메시지 루트 기준 경로. LoRa의 object.TempC_SHT 등을 그대로 사용한다.
-- COMPONENT: component_path로 배열을 선택하고 각 항목에서 payload_field와 index/uuid 경로를 읽는다.
-- 예: component_type=GPU, component_path=gpus, payload_field=gpu_temperature,
--     component_index_field=gpu_index, component_uuid_field=gpu_uuid.
-- 실제 GPU 개수·인덱스·UUID별 행을 생성하지 않는다. 페이지별 노출은 기존
-- device_page_model_point_setting(protocol=mqtt)의 point_id로 관리한다.
CREATE TABLE IF NOT EXISTS `device_model_mqtt_point` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'MQTT 모델 포인트 ID',
  `device_model_id` int(11) NOT NULL COMMENT 'device_model.id (FK)',
  `payload_field` varchar(255) NOT NULL COMMENT '루트 또는 구성요소 항목 기준 값의 JSON 경로',
  `point_name` varchar(255) NOT NULL COMMENT 'Influx point_name 태그',
  `data_point_type_id` int(11) NOT NULL COMMENT 'common_code.id (code_group=DATA_POINT_TYPE)',
  `category_code_id` int(11) DEFAULT NULL COMMENT 'common_code.id (code_group=CATEGORY, 측정 항목 분류)',
  `unit_code_id` int(11) DEFAULT NULL COMMENT 'common_code.id (code_group=UNIT)',
  `component_scope` varchar(20) NOT NULL DEFAULT 'DEVICE' COMMENT 'DEVICE 또는 COMPONENT',
  `component_type` varchar(50) DEFAULT NULL COMMENT '구성요소 종류, 예: GPU, CPU',
  `component_path` varchar(255) NOT NULL DEFAULT '' COMMENT '구성요소 배열 경로, DEVICE 범위는 빈 문자열',
  `component_index_field` varchar(255) DEFAULT NULL COMMENT '항목 내 논리 슬롯 식별 경로, 예: gpu_index',
  `component_uuid_field` varchar(255) DEFAULT NULL COMMENT '항목 내 실제 부품 식별 경로, 예: gpu_uuid',
  `scale` double DEFAULT NULL COMMENT '배율 (NULL=1)',
  `value_map` varchar(1000) DEFAULT NULL COMMENT '문자열/상태값 숫자 매핑 JSON',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '모델 포인트 사용 여부',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '포인트 생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '포인트 수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_model_mqtt_point_model_field` (`device_model_id`,`component_path`,`payload_field`),
  UNIQUE KEY `uk_device_model_mqtt_point_model_name` (`device_model_id`,`point_name`),
  KEY `idx_device_model_mqtt_point_data_point_type_id` (`data_point_type_id`),
  KEY `idx_device_model_mqtt_point_category_code_id` (`category_code_id`),
  KEY `idx_device_model_mqtt_point_unit_code_id` (`unit_code_id`),
  CONSTRAINT `fk_device_model_mqtt_point_model_id` FOREIGN KEY (`device_model_id`) REFERENCES `device_model` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_mqtt_point_data_point_type_id` FOREIGN KEY (`data_point_type_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_mqtt_point_category_code_id` FOREIGN KEY (`category_code_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_mqtt_point_unit_code_id` FOREIGN KEY (`unit_code_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_device_model_mqtt_point_enabled` CHECK (`enabled` IN (0,1)),
  CONSTRAINT `chk_device_model_mqtt_point_value_map_json` CHECK (`value_map` IS NULL OR json_valid(`value_map`)),
  CONSTRAINT `chk_device_model_mqtt_point_scope` CHECK (`component_scope` IN ('DEVICE','COMPONENT')),
  CONSTRAINT `chk_device_model_mqtt_point_component` CHECK (
    (`component_scope` = 'DEVICE' AND `component_type` IS NULL AND `component_path` = ''
      AND `component_index_field` IS NULL AND `component_uuid_field` IS NULL)
    OR (`component_scope` = 'COMPONENT' AND `component_type` IS NOT NULL AND CHAR_LENGTH(TRIM(`component_type`)) > 0
      AND CHAR_LENGTH(TRIM(`component_path`)) > 0 AND `component_index_field` IS NOT NULL
      AND CHAR_LENGTH(TRIM(`component_index_field`)) > 0)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='모델별 MQTT 포인트 정의 (LoRa 및 반복 구성요소 지원)';
-- END 54_device_model_mqtt_point.sql

-- BEGIN 55_mqtt_source.sql
-- 신규 DB용. 기존 DB는 migration/20261014_unify_mqtt_sources.sql 사용.
CREATE TABLE IF NOT EXISTS `mqtt_source` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'MQTT 수집 소스 ID',
  `name` varchar(100) NOT NULL COMMENT '운영 표시명',
  `source_type` varchar(30) NOT NULL DEFAULT 'CHIRPSTACK_LORA' COMMENT 'CHIRPSTACK_LORA 또는 AGENT',
  `broker_url` varchar(500) NOT NULL COMMENT '브로커 접속 URL',
  `topic` varchar(500) NOT NULL COMMENT 'MQTT 구독 필터',
  `client_id` varchar(255) DEFAULT NULL COMMENT 'MQTT 클라이언트 식별자',
  `credential_key` varchar(100) DEFAULT NULL COMMENT 'Sensor Data 자격증명 키; 비밀번호 미저장',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '수집 소스 활성 여부',
  `config_version` bigint(20) NOT NULL DEFAULT 1 COMMENT '설정 변경 감지용 버전',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '소스 생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '소스 수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mqtt_source_name` (`name`),
  CONSTRAINT `chk_mqtt_source_type` CHECK (`source_type` IN ('CHIRPSTACK_LORA','AGENT')),
  CONSTRAINT `chk_mqtt_source_enabled` CHECK (`enabled` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공통 MQTT 수집 소스 (LoRa·에이전트)';
-- END 55_mqtt_source.sql

-- BEGIN 56_mqtt_source_status.sql
CREATE TABLE IF NOT EXISTS `mqtt_source_status` (
  `source_id` int(11) NOT NULL COMMENT 'mqtt_source.id (PK/FK)',
  `status` varchar(30) NOT NULL DEFAULT 'NOT_SYNCED' COMMENT '수집 연결 상태',
  `last_connected_at` timestamp(6) NULL DEFAULT NULL COMMENT '마지막 MQTT 연결 시각',
  `last_message_at` timestamp(6) NULL DEFAULT NULL COMMENT '마지막 메시지 수신 시각',
  `last_status_at` timestamp(6) NULL DEFAULT NULL COMMENT '마지막 상태 보고 시각',
  `message_count` bigint(20) NOT NULL DEFAULT 0 COMMENT '누적 수신 메시지 수',
  `error_count` bigint(20) NOT NULL DEFAULT 0 COMMENT '누적 오류 수',
  `reconnect_count` bigint(20) NOT NULL DEFAULT 0 COMMENT '누적 재연결 횟수',
  `last_error` varchar(2000) DEFAULT NULL COMMENT '최근 연결/수신 오류',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '상태 레코드 생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '상태 레코드 수정 시각',
  PRIMARY KEY (`source_id`),
  CONSTRAINT `fk_mqtt_source_status_source` FOREIGN KEY (`source_id`) REFERENCES `mqtt_source` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_mqtt_source_status` CHECK (`status` IN ('NOT_SYNCED','CONNECTING','CONNECTED','DISCONNECTED','ERROR','DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공통 MQTT 수집 소스 연결/수신 상태';
-- END 56_mqtt_source_status.sql

-- BEGIN 57_threshold_rule.sql
CREATE TABLE IF NOT EXISTS `threshold_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '임계값 규칙 고유 ID',
  `device_id` int(11) NOT NULL COMMENT '임계값을 적용할 장비 ID',
  `protocol_type_id` int(11) NOT NULL COMMENT '측정값을 수집한 프로토콜 공통코드 ID',
  `point_name` varchar(120) NOT NULL COMMENT '장비의 측정 포인트 이름',
  `operator` varchar(10) NOT NULL COMMENT '경보 비교 연산자: ABOVE(초과), BELOW(미만)',
  `warning_threshold_value` decimal(24,8) NULL COMMENT '주의 경보 기준값 (미사용 시 NULL)',
  `critical_threshold_value` decimal(24,8) NULL COMMENT '위험 경보 기준값 (미사용 시 NULL)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '규칙 활성 여부',
  `created_dt` timestamp(6) NOT NULL DEFAULT current_timestamp(6) COMMENT '규칙 생성 시각',
  `updated_dt` timestamp(6) NOT NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '규칙 최종 수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_threshold_rule_device_point` (`device_id`,`point_name`),
  KEY `idx_threshold_rule_enabled` (`enabled`),
  CONSTRAINT `fk_threshold_rule_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_threshold_rule_protocol_type` FOREIGN KEY (`protocol_type_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_threshold_rule_operator` CHECK (`operator` IN ('ABOVE','BELOW')),
  CONSTRAINT `chk_threshold_rule_has_level` CHECK (`warning_threshold_value` IS NOT NULL OR `critical_threshold_value` IS NOT NULL),
  CONSTRAINT `chk_threshold_rule_level_order` CHECK (`warning_threshold_value` IS NULL OR `critical_threshold_value` IS NULL OR (`operator` = 'ABOVE' AND `critical_threshold_value` > `warning_threshold_value`) OR (`operator` = 'BELOW' AND `critical_threshold_value` < `warning_threshold_value`)),
  CONSTRAINT `chk_threshold_rule_enabled` CHECK (`enabled` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비·측정항목별 주의·위험 임계값 규칙';
-- END 57_threshold_rule.sql

-- BEGIN 58_alarm_event.sql
CREATE TABLE IF NOT EXISTS `alarm_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '알람 이벤트 고유 ID',
  `alarm_type` varchar(40) NOT NULL COMMENT '알람 발생 원인 유형',
  `severity` varchar(20) NOT NULL COMMENT '알람 심각도',
  `visibility` varchar(20) NOT NULL COMMENT '알람 조회 권한 범위 (사용자 공개 또는 관리자 전용)',
  `raised_dt` timestamp(6) NOT NULL COMMENT '알람 발생 시각',
  `cleared_dt` timestamp(6) NULL DEFAULT NULL COMMENT '중복 알람 방지용 내부 해소 시각 (미해소 시 NULL, 화면/API에는 상태로 노출하지 않음)',
  PRIMARY KEY (`id`),
  KEY `idx_alarm_event_visibility_raised` (`visibility`,`raised_dt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='통합 알람 발생 이력';
-- END 58_alarm_event.sql

-- BEGIN 59_threshold_alarm_event.sql
CREATE TABLE IF NOT EXISTS `threshold_alarm_event` (
  `event_id` bigint NOT NULL COMMENT 'alarm_event.id 및 상세 레코드 ID',
  `threshold_rule_id` bigint NOT NULL COMMENT '알람을 발생시킨 임계값 규칙 ID',
  `device_id` int(11) NOT NULL COMMENT '알람 발생 당시 장비 ID',
  `point_name` varchar(120) NOT NULL COMMENT '알람 발생 당시 측정 포인트',
  `unit` varchar(40) NOT NULL COMMENT '알람 발생 당시 측정 단위',
  `operator` varchar(10) NOT NULL COMMENT '알람 발생 당시 비교 연산자',
  `threshold_value` decimal(24,8) NOT NULL COMMENT '알람 발생 당시 임계값',
  `measured_value` decimal(24,8) NOT NULL COMMENT '알람 발생 당시 측정값',
  PRIMARY KEY (`event_id`),
  KEY `idx_threshold_alarm_event_rule` (`threshold_rule_id`),
  KEY `idx_threshold_alarm_event_device` (`device_id`),
  CONSTRAINT `fk_threshold_alarm_event_event` FOREIGN KEY (`event_id`) REFERENCES `alarm_event` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_threshold_alarm_event_rule` FOREIGN KEY (`threshold_rule_id`) REFERENCES `threshold_rule` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_threshold_alarm_event_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_threshold_alarm_event_operator` CHECK (`operator` IN ('ABOVE','BELOW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='임계값 알람 발생 당시 측정값';
-- END 59_threshold_alarm_event.sql

-- BEGIN 60_collection_gap_alarm_event.sql
CREATE TABLE IF NOT EXISTS `collection_gap_alarm_event` (
  `event_id` bigint NOT NULL COMMENT 'alarm_event.id 및 상세 레코드 ID',
  `device_id` int(11) NOT NULL COMMENT '수집 누락이 감지된 장비 ID',
  `protocol` varchar(20) NOT NULL COMMENT '수집 누락을 감시하는 통신 프로토콜 코드',
  PRIMARY KEY (`event_id`),
  KEY `idx_collection_gap_alarm_event_device_protocol` (`device_id`,`protocol`),
  CONSTRAINT `fk_collection_gap_alarm_event_event` FOREIGN KEY (`event_id`) REFERENCES `alarm_event` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_collection_gap_alarm_event_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 데이터 수집 누락 알람 상세';
-- END 60_collection_gap_alarm_event.sql

-- BEGIN 61_cctv_camera.sql
CREATE TABLE IF NOT EXISTS `cctv_camera` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'CCTV 카메라 ID',
  `camera_key` varchar(50) NOT NULL COMMENT '카메라 고유 코드',
  `name` varchar(100) NOT NULL COMMENT '화면 표시 이름',
  `stream_url` varchar(1000) NOT NULL COMMENT '브라우저에서 열 CCTV 보기 URL',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '위젯에서 선택 및 표시할지 여부',
  `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '카메라 목록 표시 순서',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cctv_camera_camera_key` (`camera_key`),
  CONSTRAINT `chk_cctv_camera_enabled` CHECK (`enabled` in (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='위젯에서 사용할 CCTV 카메라 카탈로그';
-- END 61_cctv_camera.sql

-- BEGIN 62_page_widget_cctv.sql
CREATE TABLE IF NOT EXISTS `page_widget_cctv` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'CCTV 위젯 카메라 매핑 ID',
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `camera_id` int(11) NOT NULL COMMENT 'cctv_camera.id',
  `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '위젯 내 카메라 전환 버튼 순서',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_cctv_widget_camera` (`widget_id`,`camera_id`),
  KEY `idx_page_widget_cctv_camera` (`camera_id`),
  CONSTRAINT `fk_page_widget_cctv_widget` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_page_widget_cctv_camera` FOREIGN KEY (`camera_id`) REFERENCES `cctv_camera` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CCTV 위젯과 표시 카메라 연결';
-- END 62_page_widget_cctv.sql

-- BEGIN 63_device_monitoring_policy.sql
-- 63_device_monitoring_policy.sql — per-device data monitoring mode
CREATE TABLE IF NOT EXISTS `device_monitoring_policy` (
  `device_id` int(11) NOT NULL COMMENT '감시 정책이 적용되는 devices.id',
  `monitoring_mode_code_id` int(11) NOT NULL COMMENT 'DEVICE_MONITORING_MODE 공통코드 ID',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`device_id`),
  KEY `idx_device_monitoring_policy_mode` (`monitoring_mode_code_id`),
  CONSTRAINT `fk_device_monitoring_policy_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_monitoring_policy_mode` FOREIGN KEY (`monitoring_mode_code_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비별 데이터 감시·누락 판정 방식';
-- END 63_device_monitoring_policy.sql

-- BEGIN 64_device_page_visualization.sql
-- 페이지별 보기 모드와 장면 배치. 자세한 DDL은 migration/20261022_device_page_visualization.sql 참조.
CREATE TABLE IF NOT EXISTS `device_page_display_config` (
  `page_code_id` int(11) NOT NULL COMMENT 'DEVICE_PAGE 공통코드 ID',
  `view_mode` varchar(16) NOT NULL COMMENT '페이지 표시 방식 (DATA/PLAN_2D/SCENE_3D)',
  `layout_asset_key` varchar(120) DEFAULT NULL COMMENT '선택한 평면도/장면 자산 경로',
  `location_hierarchy_initialized` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Location hierarchy initialization status',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`page_code_id`),
  CONSTRAINT `fk_device_page_display_config_page` FOREIGN KEY (`page_code_id`)
    REFERENCES `common_code` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='페이지별 데이터/2D/3D 표시 방식';

CREATE TABLE IF NOT EXISTS `device_page_scene_item` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '장면 객체 ID',
  `page_code_id` int(11) NOT NULL COMMENT '객체가 배치되는 DEVICE_PAGE 공통코드 ID',
  `parent_item_id` int(11) DEFAULT NULL COMMENT '부모 객체 ID (센서를 장착한 랙)',
  `device_id` int(11) DEFAULT NULL COMMENT '연결된 장비 ID (장면용 빈 랙은 NULL)',
  `location_node_code` char(10) DEFAULT NULL COMMENT '실제 랙 위치 노드 코드 (LOCATION_TYPE=RACK)',
  `item_type` varchar(16) NOT NULL COMMENT '장면 객체 종류 (RACK/EQUIPMENT/DEVICE/SENSOR)',
  `point_name` varchar(100) DEFAULT NULL COMMENT '센서 마커가 표시할 측정 포인트 이름',
  `label` varchar(120) NOT NULL COMMENT '장면에 표시할 객체 이름',
  `model_key` varchar(32) NOT NULL COMMENT '장면 모델 카탈로그 키 (랙/장비/벽/문/창문/천장/바닥/조명/배관 등)',
  `rack_color` varchar(7) DEFAULT NULL COMMENT '랙 표시 색상 (#RRGGBB)',
  `position_x` double NOT NULL COMMENT '장면 X 좌표(랙/장비: 방 기준 미터, 센서: 랙 기준 미터)',
  `position_y` double NOT NULL COMMENT '장면 Y 높이(랙/장비: 방 기준 미터, 센서: 랙 기준 미터)',
  `position_z` double NOT NULL COMMENT '장면 Z 좌표(랙/장비: 방 기준 미터, 센서: 랙 기준 미터)',
  `rotation_y` double NOT NULL DEFAULT 0 COMMENT 'Y축 회전 각도(도)',
  `size_x` double NOT NULL DEFAULT 4 COMMENT '공간 타일 X 크기(미터, 바닥 타일)',
  `size_y` double NOT NULL DEFAULT 4 COMMENT '프레임 높이 배율 (size_y / 4)',
  `size_z` double NOT NULL DEFAULT 4 COMMENT '공간 타일 Z 크기(미터, 바닥 타일)',
  `size_axes_version` tinyint NOT NULL DEFAULT 1 COMMENT '설비 축별 크기 변환 버전',
  `group_key` varchar(36) DEFAULT NULL COMMENT '같이 이동하는 설비 묶음 식별자',
  `group_name` varchar(80) DEFAULT NULL COMMENT '설비 묶음 이름',
  `locked` tinyint(1) NOT NULL DEFAULT 0 COMMENT '설비 배치 잠금 여부',
  `panel_photo` longtext DEFAULT NULL COMMENT '분전반 내부 사진',
  `panel_layout_initialized` tinyint(1) NOT NULL DEFAULT 0 COMMENT '분전반 기본 배치 저장 여부',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  KEY `idx_device_page_scene_item_page` (`page_code_id`),
  KEY `idx_device_page_scene_item_parent` (`parent_item_id`),
  KEY `idx_device_page_scene_item_device` (`device_id`),
  KEY `idx_device_page_scene_item_location_node` (`location_node_code`),
  UNIQUE KEY `uk_device_page_scene_item_page_device_type` (`page_code_id`, `device_id`, `item_type`),
  UNIQUE KEY `uk_device_page_scene_item_page_location_type` (`page_code_id`, `location_node_code`, `item_type`),
  CONSTRAINT `fk_device_page_scene_item_page` FOREIGN KEY (`page_code_id`)
    REFERENCES `common_code` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_page_scene_item_parent` FOREIGN KEY (`parent_item_id`)
    REFERENCES `device_page_scene_item` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_page_scene_item_device` FOREIGN KEY (`device_id`)
    REFERENCES `devices` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_device_page_scene_item_location_node` FOREIGN KEY (`location_node_code`)
    REFERENCES `location_node` (`code`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='페이지 2D/3D 장면의 공간 부품·장비와 장비 부착 위치';
-- END 64_device_page_visualization.sql

-- BEGIN 65_scene_model.sql
-- 3D 도면 모델 카탈로그. 기존 SR_* 키를 유지해 저장된 도면과 호환한다.
CREATE TABLE IF NOT EXISTS `scene_model` (
  `model_key` varchar(32) NOT NULL,
  `model_name` varchar(120) NOT NULL,
  `category` varchar(50) NOT NULL DEFAULT '기타',
  `builtin_file` varchar(255) DEFAULT NULL,
  `storage_key` varchar(255) DEFAULT NULL,
  `file_name` varchar(255) NOT NULL,
  `width_m` double NOT NULL,
  `height_m` double NOT NULL,
  `depth_m` double NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `rack_u_installable` tinyint(1) NOT NULL DEFAULT 0,
  `rack_attachment_installable` tinyint(1) NOT NULL DEFAULT 0,
  `device_linkable` tinyint(1) NOT NULL DEFAULT 0,
  `panel_installable` tinyint(1) NOT NULL DEFAULT 0,
  `rack_installable` tinyint(1) NOT NULL DEFAULT 0 COMMENT '랙에 설치 가능한 3D 모델 여부',
  `scene_role` varchar(20) DEFAULT NULL COMMENT '도면 역할: RACK, PANEL, FRAME 또는 NULL',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`model_key`),
  CONSTRAINT `ck_scene_model_source` CHECK ((`builtin_file` IS NULL) <> (`storage_key` IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @scene_model_initial_seed = (SELECT COUNT(*) = 0 FROM `scene_model`);
INSERT IGNORE INTO `scene_model`
  (`model_key`, `model_name`, `builtin_file`, `file_name`, `width_m`, `height_m`, `depth_m`) VALUES
  ('SR_RACK', '서버 랙 (전면 도어)', 'Inrow_Cooling_Rack.glb', 'Inrow_Cooling_Rack.glb', .9, 2.35, .9),
  ('SR_RDC', 'RDC 장비', 'rdc.glb', 'rdc.glb', 1.2, 2.1, 1.1),
  ('SR_INROW', '인로우 냉각기', 'inrow-cooler.glb', 'inrow-cooler.glb', .9, 2.35, .9),
  ('SR_INROW_MOLD', '인로우 프레임', 'in_row_frame.glb', 'in_row_frame.glb', 1.05, 1.05, 1.45),
  ('SR_INROW_PANEL', '인로우 분전반', 'in_row_distribution_panel.glb', 'in_row_distribution_panel.glb', 1, 1.77, .4),
  ('SR_WALL_COIL', '월 코일 프레임', 'wall_coil_mold.glb', 'wall_coil_mold.glb', 2.8, 2.2, 1.6),
  ('SR_DLC_RACK', 'DLC 랙', 'DLC_Rack.glb', 'DLC_Rack.glb', .9, 2.35, .9),
  ('SR_INROW_RACK', '인로우 냉각 랙', 'Inrow_Cooling_Rack.glb', 'Inrow_Cooling_Rack.glb', .9, 2.35, .9),
  ('SR_OCP_RACK', 'OCP 랙', 'OCP_Rack.glb', 'OCP_Rack.glb', .9, 2.35, .9),
  ('SR_RDC_RACK', 'RDC 랙', 'RDC_Rack.glb', 'RDC_Rack.glb', .9, 2.35, .9),
  ('SR_ORV3_RACK', 'ORV3 랙', 'ORV3_Rack.glb', 'ORV3_Rack.glb', .9, 2.35, .9),
  ('SR_IMMERSION', '액침 냉각 설비', 'Immersion_Cooling.glb', 'Immersion_Cooling.glb', 1.4, 1.5, 1.2),
  ('SR_HAC_RACK', 'HAC 랙', 'HAC_Rack.glb', 'HAC_Rack.glb', .9, 2.35, .9),
  ('SR_DLC_CDU', 'DLC CDU', 'DLC_CDU_CHx200.glb', 'DLC_CDU_CHx200.glb', 1.2, 2, 1),
  ('SR_DLC_SERVER', 'DLC 서버', 'DLC_Server_SuperMicro SYS-422GS-NBRT-LCC.glb', 'DLC_Server_SuperMicro SYS-422GS-NBRT-LCC.glb', 1, 1.2, 1),
  ('SR_PDU_RACK', '랙 장착형 PDU', 'pdu_rack_horizontal.glb', 'pdu_rack_horizontal.glb', .482, .11, .24),
  ('SR_PDU_VERTICAL', '세로형 PDU', 'pdu_rack_vertical.glb', 'pdu_rack_vertical.glb', .1, 1.6, .1),
  ('SR_GPU_SERVER', 'GPU 서버 (4U 예시)', 'gpu_server_rack.glb', 'gpu_server_rack.glb', .482, .178, .72),
  ('SR_DRAGINO_TH', 'Dragino 온습도 센서', 'dragino_temperature_humidity.glb', 'dragino_temperature_humidity.glb', .068, .12, .027),
  ('SR_DRAGINO_DOOR', 'Dragino 도어 센서', 'dragino_door_contact.glb', 'dragino_door_contact.glb', .13, .10, .03),
  ('SR_DOUBLE_RACK', '서버 랙 (양문형)', 'rack_double_door.glb', 'rack_double_door.glb', .9, 2.35, .9),
  ('SR_ACCURA_2350_1P', 'ACCURA 2350 단상', 'accura_2350_1p.glb', 'accura_2350_1p.glb', .045, .105, .045),
  ('SR_ACCURA_2350_3P', 'ACCURA 2350 3상', 'accura_2350_3p.glb', 'accura_2350_3p.glb', .060, .105, .045),
  ('SR_PANEL_BREAKER', '분전반 차단기', 'panel_breaker_3p.glb', 'panel_breaker_3p.glb', .095, .11, .075),
  ('SR_PANEL_BRANCH', '분전반 분기 차단기', 'panel_breaker_3p.glb', 'panel_breaker_3p.glb', .34, .14, .12),
  ('SR_PANEL_MAIN', '분전반 메인 차단기', 'panel_main_breaker.glb', 'panel_main_breaker.glb', .37, .31, .13),
  ('SR_PANEL_METER', '분전반 계기', 'panel_meter.glb', 'panel_meter.glb', .36, .23, .12),
  ('SR_PANEL_BUSBAR', '분전반 부스바', 'panel_busbar.glb', 'panel_busbar.glb', .06, .29, .04),
  ('SR_PANEL_WIRE', '분전반 배선', 'panel_wire.glb', 'panel_wire.glb', .08, .13, .012);

UPDATE `scene_model` SET `category` = CASE
  WHEN `model_key` IN ('SR_DRAGINO_TH', 'SR_DRAGINO_DOOR', 'SR_ACCURA_2350_1P', 'SR_ACCURA_2350_3P') THEN '센서'
  WHEN `model_key` IN ('SR_INROW_MOLD', 'SR_WALL_COIL') THEN '프레임·구조물'
  WHEN `model_key` IN ('SR_RACK', 'SR_DOUBLE_RACK', 'SR_INROW_RACK', 'SR_DLC_RACK', 'SR_OCP_RACK', 'SR_RDC_RACK', 'SR_ORV3_RACK', 'SR_HAC_RACK', 'SR_DLC_SERVER', 'SR_GPU_SERVER') THEN '랙·서버'
  WHEN `model_key` IN ('SR_INROW', 'SR_IMMERSION', 'SR_DLC_CDU') THEN '냉각 설비'
  WHEN `model_key` IN ('SR_INROW_PANEL', 'SR_RDC', 'SR_PDU_RACK', 'SR_PDU_VERTICAL') THEN '전력 설비'
  ELSE `category` END
WHERE `category` IN ('기타', '인로우') AND `model_key` LIKE 'SR_%';

UPDATE `scene_model` SET `rack_installable` = 1
WHERE @scene_model_initial_seed = 1 AND `model_key` IN ('SR_GPU_SERVER', 'SR_DLC_SERVER', 'SR_PDU_RACK', 'SR_PDU_VERTICAL', 'SR_DRAGINO_TH', 'SR_DRAGINO_DOOR');

UPDATE `scene_model` SET `rack_u_installable` = 1 WHERE @scene_model_initial_seed = 1 AND `model_key` IN ('SR_GPU_SERVER','SR_DLC_SERVER','SR_PDU_RACK');
UPDATE `scene_model` SET `rack_attachment_installable` = 1 WHERE @scene_model_initial_seed = 1 AND `model_key` IN ('SR_PDU_VERTICAL','SR_DRAGINO_TH','SR_DRAGINO_DOOR');
UPDATE `scene_model` SET `device_linkable` = 1 WHERE @scene_model_initial_seed = 1 AND `model_key` IN ('SR_RDC','SR_IMMERSION','SR_RDC_RACK');
UPDATE `scene_model` SET `panel_installable` = 1 WHERE @scene_model_initial_seed = 1 AND `model_key` IN ('SR_ACCURA_2350_1P','SR_ACCURA_2350_3P','SR_PANEL_BREAKER','SR_PANEL_BRANCH','SR_PANEL_MAIN','SR_PANEL_METER','SR_PANEL_BUSBAR','SR_PANEL_WIRE');
UPDATE `scene_model` SET `scene_role` = 'RACK' WHERE @scene_model_initial_seed = 1 AND `model_key` IN ('SR_RACK','SR_DOUBLE_RACK','SR_DLC_RACK','SR_INROW_RACK','SR_OCP_RACK','SR_RDC_RACK','SR_ORV3_RACK','SR_HAC_RACK');
UPDATE `scene_model` SET `scene_role` = 'PANEL' WHERE @scene_model_initial_seed = 1 AND `model_key` = 'SR_INROW_PANEL';
UPDATE `scene_model` SET `scene_role` = 'FRAME' WHERE @scene_model_initial_seed = 1 AND `model_key` IN ('SR_INROW_MOLD','SR_WALL_COIL');
-- END 65_scene_model.sql

-- BEGIN 66_scene_builtin_asset.sql
-- Procedural 3D assets rendered by the client. Keys and categories match the renderer.
CREATE TABLE IF NOT EXISTS `scene_builtin_asset` (
  `asset_key` varchar(32) NOT NULL,
  `asset_name` varchar(120) NOT NULL,
  `category` varchar(20) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`asset_key`),
  CONSTRAINT `ck_scene_builtin_asset_category` CHECK (`category` IN ('ARCHITECTURE', 'FACTORY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `scene_builtin_asset` (`asset_key`, `asset_name`, `category`) VALUES
  ('WALL', '기본 벽', 'ARCHITECTURE'),
  ('WALL_CONCRETE', '콘크리트 벽', 'ARCHITECTURE'),
  ('WALL_GLASS', '유리 벽', 'ARCHITECTURE'),
  ('WALL_GLASS_CLEAR', '통유리 벽', 'ARCHITECTURE'),
  ('WALL_PANEL', '패널 벽', 'ARCHITECTURE'),
  ('DOOR', '문', 'ARCHITECTURE'),
  ('DOOR_DOUBLE', '양문', 'ARCHITECTURE'),
  ('DOOR_SLIDING', '미닫이문', 'ARCHITECTURE'),
  ('WINDOW', '창문', 'ARCHITECTURE'),
  ('WINDOW_DOUBLE', '큰 창문', 'ARCHITECTURE'),
  ('FLOOR', '바닥', 'ARCHITECTURE'),
  ('CEILING', '유리 천장', 'ARCHITECTURE'),
  ('CEILING_CLEAR', '투명 유리 천장', 'ARCHITECTURE'),
  ('LIGHT', '조명', 'ARCHITECTURE'),
  ('PIPE', '배관', 'ARCHITECTURE'),
  ('STAIR', '계단', 'ARCHITECTURE'),
  ('MACHINE', '생산 설비', 'FACTORY'),
  ('CONVEYOR', '컨베이어', 'FACTORY'),
  ('TANK', '저장 탱크', 'FACTORY'),
  ('TABLE', '작업대', 'FACTORY');
-- END 66_scene_builtin_asset.sql

-- BEGIN 67_rack_model_placement.sql
CREATE TABLE IF NOT EXISTS `rack_model_placement` (
  `id` int NOT NULL AUTO_INCREMENT,
  `rack_location_code` char(10) NOT NULL,
  `scene_model_key` varchar(32) NOT NULL,
  `mount_type` varchar(30) NOT NULL,
  `rack_side` varchar(20) DEFAULT NULL,
  `u_position` int DEFAULT NULL,
  `u_height` int DEFAULT NULL,
  `position_x` double DEFAULT NULL,
  `position_y` double DEFAULT NULL,
  `position_z` double DEFAULT NULL,
  `visual_length_ratio` double DEFAULT NULL COMMENT '측면 장착 3D 모델의 랙 높이 대비 길이',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`id`),
  KEY `idx_rack_model_location` (`rack_location_code`),
  CONSTRAINT `fk_rack_model_location` FOREIGN KEY (`rack_location_code`) REFERENCES `location_node` (`code`) ON DELETE CASCADE,
  CONSTRAINT `fk_rack_model_scene` FOREIGN KEY (`scene_model_key`) REFERENCES `scene_model` (`model_key`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='실제 장비 연결 전 랙 모델 배치';
-- END 67_rack_model_placement.sql

-- BEGIN 68_rack_widget_config.sql
CREATE TABLE IF NOT EXISTS rack_widget_config (
  rack_location_code VARCHAR(10) NOT NULL,
  widgets_json LONGTEXT NOT NULL,
  updated_dt TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (rack_location_code),
  CONSTRAINT fk_rack_widget_config_location FOREIGN KEY (rack_location_code) REFERENCES location_node(code) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- END 68_rack_widget_config.sql

-- BEGIN 69_initial_admin_lock.sql
-- Stable row serializes first administrator registration across Manager instances.
CREATE TABLE IF NOT EXISTS `initial_admin_lock` (
  `id` int NOT NULL PRIMARY KEY COMMENT '초기 관리자 등록 잠금 키'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='초기 관리자 등록 직렬화 잠금';
INSERT IGNORE INTO `initial_admin_lock` (`id`) VALUES (1);
-- END 69_initial_admin_lock.sql

-- BEGIN 69_rack_scene_display_config.sql
CREATE TABLE IF NOT EXISTS rack_scene_display_config (
  rack_location_code VARCHAR(10) NOT NULL,
  display_json LONGTEXT NOT NULL,
  updated_dt TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (rack_location_code),
  CONSTRAINT fk_rack_scene_display_location FOREIGN KEY (rack_location_code) REFERENCES location_node(code) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- END 69_rack_scene_display_config.sql

-- BEGIN seed/model_catalog.sql
-- 새 현장 배포용 모델 카탈로그 단일 seed
-- 생성일: 2026-09-14
-- 범위: 공통 코드, 장비 모델, 모델-프로토콜, SNMP/Modbus 포인트
-- 제외: 위치, 실제 장비, endpoint, 수집 작업, 위젯, PUE, 자산/Rack, InfluxDB 이력
-- 전제: sql/schema/02~08 DDL이 적용된 빈 DB에서 실행한다.
-- 주의: FK ID를 보존하므로 기존 운영 데이터가 있는 DB에는 실행하지 않는다.

SET NAMES utf8mb4;

INSERT INTO `code_group` (`id`, `group_key`, `group_name`) VALUES
(2,CONVERT(0x50524F544F434F4C5F54595045 USING utf8mb4),CONVERT(0x50726F746F636F6C2054797065 USING utf8mb4)),
(3,CONVERT(0x4C4F434154494F4E5F54595045 USING utf8mb4),CONVERT(0x4C6F636174696F6E2054797065 USING utf8mb4)),
(4,CONVERT(0x4C4F434154494F4E5F50415448 USING utf8mb4),CONVERT(0x4C6F636174696F6E2050617468 USING utf8mb4)),
(5,CONVERT(0x444154415F504F494E545F54595045 USING utf8mb4),CONVERT(0x4461746120506F696E742054797065 USING utf8mb4)),
(6,CONVERT(0x4445564943455F50414745 USING utf8mb4),CONVERT(0x4465766963652050616765 USING utf8mb4)),
(8,CONVERT(0x4D4F44454C5F54595045 USING utf8mb4),CONVERT(0x4D6F64656C2054797065 USING utf8mb4)),
(9,CONVERT(0x41535345545F535441545553 USING utf8mb4),CONVERT(0xEC9E90EC82B020EC9AB4EC988120EC8381ED839C USING utf8mb4)),
(10,'UNIT',CONVERT(0xECB8A1ECA09520EB8BA8EC9C84 USING utf8mb4));

INSERT INTO `common_code` (`id`, `group_id`, `code`, `name`, `sort_order`) VALUES
(1,5,CONVERT(0x504F574552 USING utf8mb4),CONVERT(0x504F574552 USING utf8mb4),1),
(2,5,CONVERT(0x454E45524759 USING utf8mb4),CONVERT(0x454E45524759 USING utf8mb4),2),
(3,5,CONVERT(0x43555252454E54 USING utf8mb4),CONVERT(0x43555252454E54 USING utf8mb4),3),
(4,5,CONVERT(0x564F4C54414745 USING utf8mb4),CONVERT(0x564F4C54414745 USING utf8mb4),4),
(5,5,CONVERT(0x54454D5045524154555245 USING utf8mb4),CONVERT(0x54454D5045524154555245 USING utf8mb4),5),
(6,5,CONVERT(0x5052455353555245 USING utf8mb4),CONVERT(0x5052455353555245 USING utf8mb4),6),
(7,5,CONVERT(0x464C4F57 USING utf8mb4),CONVERT(0x464C4F57 USING utf8mb4),7),
(8,5,CONVERT(0x504F5745525F464143544F52 USING utf8mb4),CONVERT(0x504F5745525F464143544F52 USING utf8mb4),8),
(9,2,CONVERT(0x736E6D70 USING utf8mb4),CONVERT(0x534E4D50 USING utf8mb4),1),
(10,2,CONVERT(0x6D6F64627573 USING utf8mb4),CONVERT(0x4D6F64627573 USING utf8mb4),2),
(11,2,CONVERT(0x6D717474 USING utf8mb4),CONVERT(0x4D515454 USING utf8mb4),3),
(16,3,CONVERT(0x554E41535349474E4544 USING utf8mb4),CONVERT(0xEBAFB8EBB0B0ECA095 USING utf8mb4),1),
(17,3,CONVERT(0x53495445 USING utf8mb4),CONVERT(0x53697465 USING utf8mb4),2),
(18,3,CONVERT(0x524F4F4D USING utf8mb4),CONVERT(0x526F6F6D USING utf8mb4),3),
(19,4,CONVERT(0x41 USING utf8mb4),CONVERT(0x41 USING utf8mb4),1),
(20,4,CONVERT(0x42 USING utf8mb4),CONVERT(0x42 USING utf8mb4),2),
(21,4,CONVERT(0x43 USING utf8mb4),CONVERT(0x43 USING utf8mb4),3),
(22,6,CONVERT(0x64617368626F617264 USING utf8mb4),CONVERT(0x44617368626F617264 USING utf8mb4),1),
(23,6,CONVERT(0x454E5649524F4E4D454E54 USING utf8mb4),CONVERT(0x456E7669726F6E6D656E74 USING utf8mb4),2),
(24,6,CONVERT(0x504F574552 USING utf8mb4),CONVERT(0x506F776572 USING utf8mb4),3),
(27,3,CONVERT(0x5A4F4E45 USING utf8mb4),CONVERT(0x5A4F4E45 USING utf8mb4),4),
(28,3,CONVERT(0x434F4E5441494E4552 USING utf8mb4),CONVERT(0x434F4E5441494E4552 USING utf8mb4),5),
(29,3,CONVERT(0x524F57 USING utf8mb4),CONVERT(0x524F57 USING utf8mb4),6),
(30,3,CONVERT(0x5241434B USING utf8mb4),CONVERT(0x5241434B USING utf8mb4),7),
(31,8,CONVERT(0x504455 USING utf8mb4),CONVERT(0x504455 USING utf8mb4),1),
(32,8,CONVERT(0x494E2D524F572D434F4F4C4552 USING utf8mb4),CONVERT(0x494E2D524F572D434F4F4C4552 USING utf8mb4),2),
(33,8,CONVERT(0x434455 USING utf8mb4),CONVERT(0x434455 USING utf8mb4),3),
(34,8,CONVERT(0x4348494C4C4552 USING utf8mb4),CONVERT(0x4348494C4C4552 USING utf8mb4),4),
(35,5,CONVERT(0x554E434C4153534946494544 USING utf8mb4),CONVERT(0x554E434C4153534946494544 USING utf8mb4),0),
(36,9,CONVERT(0x414354495645 USING utf8mb4),CONVERT(0xEC9AB4EC9881ECA491 USING utf8mb4),1),
(37,9,CONVERT(0x4D41494E54454E414E4345 USING utf8mb4),CONVERT(0xECA090EAB28020ECA491 USING utf8mb4),3),
(38,9,CONVERT(0x494E414354495645 USING utf8mb4),CONVERT(0xEBAFB8EC82ACEC9AA9 USING utf8mb4),3),
(39,9,CONVERT(0x52455449524544 USING utf8mb4),CONVERT(0xED8F90EAB8B0 USING utf8mb4),5),
(40,8,CONVERT(0x646973747269627574696F6E2D626F617264 USING utf8mb4),CONVERT(0xEBB684ECA084EBB098 USING utf8mb4),2),
(44,9,CONVERT(0x4641554C54 USING utf8mb4),CONVERT(0xEC9EA5EC95A0 USING utf8mb4),4),
(53,10,'W','W',1),
(54,10,'KWH','kWh',2),
(55,10,'V','V',3),
(56,10,'A','A',4),
(57,10,'HZ','Hz',5),
(58,10,'CELSIUS',CONVERT(0xC2B043 USING utf8mb4),6),
(59,10,'PERCENT','%',7),
(60,10,'PA','Pa',8),
(61,10,'L_PER_MIN','L/min',9),
(62,10,'RPM','rpm',10),
(63,10,'DBM','dBm',11),
(64,10,'PPM','ppm',12),
(65,10,'KVA','kVA',13),
(66,10,'KPA','kPa',14),
(67,10,'PSI','PSI',15),
(68,10,'PUE','PUE',16),
(69,10,'STATUS','status',17);

INSERT INTO `device_model` (`id`, `name`, `manufacturer`, `device_type_id`, `description`) VALUES
(1,CONVERT(0x5044552D337068617365 USING utf8mb4),CONVERT(0x5261726974616E USING utf8mb4),31,NULL),
(2,CONVERT(0x5044552D317068617365 USING utf8mb4),CONVERT(0x7261726974616E USING utf8mb4),31,NULL),
(3,CONVERT(0xED95ADEC98A8ED95ADEC8AB5EAB8B0 USING utf8mb4),CONVERT(0x566572746976 USING utf8mb4),32,NULL);

INSERT INTO `device_model_protocol` (`id`, `model_id`, `protocol_type_id`) VALUES
(1,1,9),
(2,2,9),
(3,3,9);

INSERT INTO `device_model_snmp_point`
(`id`, `model_protocol_id`, `name`, `data_point_type_id`, `oid`, `requires_instance`, `unit_code_id`, `scale`, `enabled`) VALUES
(1,1,CONVERT(0x544F54414C5F5754 USING utf8mb4),1,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E332E312E342E312E312E35 USING utf8mb4),0,53,NULL,1),
(2,1,CONVERT(0x414D50 USING utf8mb4),3,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E332E312E342E312E312E31 USING utf8mb4),0,56,0.001,1),
(3,1,CONVERT(0x5046 USING utf8mb4),8,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E332E312E342E312E312E37 USING utf8mb4),0,59,0.01,1),
(4,1,CONVERT(0x544F54414C5F4B5748 USING utf8mb4),2,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E332E312E342E312E312E38 USING utf8mb4),0,54,0.001,1),
(5,1,CONVERT(0x4C315F57415454 USING utf8mb4),1,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E342E312E342E312E312E312E35 USING utf8mb4),0,53,NULL,1),
(6,1,CONVERT(0x4C325F57415454 USING utf8mb4),1,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E342E312E342E312E312E322E35 USING utf8mb4),0,53,NULL,1),
(7,1,CONVERT(0x4C335F57415454 USING utf8mb4),1,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E342E312E342E312E312E332E35 USING utf8mb4),0,53,NULL,1),
(8,1,CONVERT(0x4C315F414D50 USING utf8mb4),3,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E342E312E342E312E312E312E31 USING utf8mb4),0,56,0.001,1),
(9,1,CONVERT(0x4C325F414D50 USING utf8mb4),3,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E342E312E342E312E312E322E31 USING utf8mb4),0,56,0.001,1),
(10,1,CONVERT(0x4C335F414D50 USING utf8mb4),3,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E342E312E342E312E312E332E31 USING utf8mb4),0,56,0.001,1),
(11,1,CONVERT(0x4C315F4B5748 USING utf8mb4),2,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E342E312E342E312E312E312E38 USING utf8mb4),0,54,0.001,1),
(12,1,CONVERT(0x4C325F4B5748 USING utf8mb4),2,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E342E312E342E312E312E322E38 USING utf8mb4),0,54,0.001,1),
(13,1,CONVERT(0x4C335F4B5748 USING utf8mb4),2,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E342E312E342E312E312E332E38 USING utf8mb4),0,54,0.001,1),
(14,2,CONVERT(0x414D50 USING utf8mb4),3,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E332E312E342E312E312E31 USING utf8mb4),0,56,0.001,1),
(15,2,CONVERT(0x5046 USING utf8mb4),8,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E332E312E342E312E312E37 USING utf8mb4),0,59,0.01,1),
(16,2,CONVERT(0x544F54414C5F5754 USING utf8mb4),1,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E332E312E342E312E312E35 USING utf8mb4),0,53,NULL,1),
(17,2,CONVERT(0x544F54414C5F4B5748 USING utf8mb4),2,CONVERT(0x2E312E332E362E312E342E312E31333734322E362E352E322E332E312E342E312E312E38 USING utf8mb4),0,54,0.001,1),
(18,3,CONVERT(0x535441545553 USING utf8mb4),35,CONVERT(0x2E312E332E362E312E342E312E31333430302E322E333039322E322E322E37362E30 USING utf8mb4),0,NULL,NULL,1),
(19,3,CONVERT(0x434F4F4C494E472D535441545553 USING utf8mb4),35,CONVERT(0x2E312E332E362E312E342E312E31333430302E322E333039322E322E322E37382E30 USING utf8mb4),0,NULL,NULL,1),
(20,3,CONVERT(0x434F4D502D4341504143495459 USING utf8mb4),35,CONVERT(0x2E312E332E362E312E342E312E31333430302E322E333039322E322E322E3132352E30 USING utf8mb4),0,59,0.1,1),
(21,3,CONVERT(0x45582D54454D50 USING utf8mb4),5,CONVERT(0x2E312E332E362E312E342E312E31333430302E322E333039322E322E322E38392E30 USING utf8mb4),0,58,0.1,1),
(22,3,CONVERT(0x45582D48554D USING utf8mb4),35,CONVERT(0x2E312E332E362E312E342E312E31333430302E322E333039322E322E322E39322E30 USING utf8mb4),0,59,0.1,1),
(23,3,CONVERT(0x494E2D54454D50 USING utf8mb4),5,CONVERT(0x2E312E332E362E312E342E312E31333430302E322E333039322E322E322E39352E30 USING utf8mb4),0,58,0.1,1),
(24,3,CONVERT(0x494E2D48554D USING utf8mb4),35,CONVERT(0x2E312E332E362E312E342E312E31333430302E322E333039322E322E322E39382E30 USING utf8mb4),0,59,0.1,1),
(25,3,CONVERT(0x46414E5F535441545553 USING utf8mb4),35,CONVERT(0x2E312E332E362E312E342E312E31333430302E322E333039322E322E322E38322E30 USING utf8mb4),0,NULL,NULL,1),
(26,3,CONVERT(0x46414E5F535045454431 USING utf8mb4),35,CONVERT(0x2E312E332E362E312E342E312E31333430302E322E333039322E322E322E3132332E30 USING utf8mb4),0,59,0.1,1),
(27,3,CONVERT(0x46414E5F535045454432 USING utf8mb4),35,CONVERT(0x2E312E332E362E312E342E312E31333430302E322E333039322E322E322E3132342E30 USING utf8mb4),0,59,0.1,1);

-- 현재 운영 DB에는 Modbus 모델 포인트가 없다.
-- Modbus 포인트가 등록된 뒤에는 아래 테이블의 행도 같은 파일에 추가한다.
-- INSERT INTO `device_model_modbus_point`
-- (`id`, `model_protocol_id`, `name`, `register_type`, `data_type`, `byte_order`,
--  `address`, `requires_instance`, `scale`, `unit`, `enabled`) VALUES (...);
-- END seed/model_catalog.sql

-- BEGIN seed/device_monitoring_mode.sql
-- 신규 DB에서 장비별 데이터 감시 모드를 사용할 때 적용
INSERT INTO `code_group` (`group_key`, `group_name`)
VALUES ('DEVICE_MONITORING_MODE', '장비 데이터 감시 방식')
ON DUPLICATE KEY UPDATE `group_name` = VALUES(`group_name`);

INSERT INTO `common_code` (`group_id`, `code`, `name`, `sort_order`, `threshold_supported`)
SELECT g.`id`, 'PERIODIC', '주기 수집', 1, 0
FROM `code_group` g WHERE g.`group_key` = 'DEVICE_MONITORING_MODE'
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `sort_order` = VALUES(`sort_order`);

INSERT INTO `common_code` (`group_id`, `code`, `name`, `sort_order`, `threshold_supported`)
SELECT g.`id`, 'ON_CHANGE', '상태 변경 수집', 2, 0
FROM `code_group` g WHERE g.`group_key` = 'DEVICE_MONITORING_MODE'
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `sort_order` = VALUES(`sort_order`);
-- END seed/device_monitoring_mode.sql

-- Runtime DataPointTypeBootstrap seed: maintain the same 10 shared point types on an empty schema.
INSERT IGNORE INTO `code_group` (`group_key`, `group_name`) VALUES ('DATA_POINT_TYPE', 'Data Point Type');
INSERT IGNORE INTO `common_code` (`group_id`, `code`, `name`, `sort_order`)
SELECT g.id, seed.code, seed.name, seed.sort_order
FROM `code_group` g
JOIN (
  SELECT 'POWER' AS code, 'POWER' AS name, 1 AS sort_order UNION ALL
  SELECT 'ENERGY', 'ENERGY', 2 UNION ALL
  SELECT 'CURRENT', 'CURRENT', 3 UNION ALL
  SELECT 'VOLTAGE', 'VOLTAGE', 4 UNION ALL
  SELECT 'TEMPERATURE', 'TEMPERATURE', 5 UNION ALL
  SELECT 'PRESSURE', 'PRESSURE', 6 UNION ALL
  SELECT 'FLOW', 'FLOW', 7 UNION ALL
  SELECT 'POWER_FACTOR', 'POWER_FACTOR', 8 UNION ALL
  SELECT 'HUMIDITY', 'HUMIDITY', 9 UNION ALL
  SELECT 'UNCLASSIFIED', 'UNCLASSIFIED', 10
) seed
WHERE g.group_key='DATA_POINT_TYPE';
