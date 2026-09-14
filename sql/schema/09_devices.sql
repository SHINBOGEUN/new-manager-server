-- 09_devices.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `devices` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '장비 ID (API {deviceId}, Influx tag device_id)',
  `model_id` int(11) NOT NULL COMMENT 'device_model.id (FK) — 제품 카탈로그',
  `location_node_code` char(10) NOT NULL COMMENT 'location_node.code (FK, 필수 — 미지정 시 UNASSIGNED)',
  `path_code_id` int(11) DEFAULT NULL COMMENT 'LOCATION_PATH common_code.id (PDU Path 피드)',
  `name` varchar(255) NOT NULL COMMENT '현장 표시명',
  `description` varchar(1000) DEFAULT NULL COMMENT '설명',
  `asset_code` varchar(100) DEFAULT NULL COMMENT '내부 자산번호',
  `serial_number` varchar(200) DEFAULT NULL COMMENT '제조사 시리얼번호',
  `asset_status_id` int(11) DEFAULT NULL COMMENT '자산 운영 상태 (ASSET_STATUS common_code)',
  `asset_color` varchar(20) DEFAULT NULL COMMENT '자산 표시 색상',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_devices_location_node_code_name` (`location_node_code`,`name`),
  UNIQUE KEY `uk_devices_asset_code` (`asset_code`),
  KEY `idx_devices_model_enabled` (`model_id`,`enabled`),
  KEY `idx_devices_location_enabled` (`location_node_code`,`enabled`),
  KEY `idx_devices_path_code_id` (`path_code_id`),
  KEY `idx_devices_asset_status_id` (`asset_status_id`),
  CONSTRAINT `fk_devices_location_node_code` FOREIGN KEY (`location_node_code`) REFERENCES `location_node` (`code`) ON UPDATE CASCADE,
  CONSTRAINT `fk_devices_model_id` FOREIGN KEY (`model_id`) REFERENCES `device_model` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_devices_path_code_id` FOREIGN KEY (`path_code_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_devices_asset_status_id` FOREIGN KEY (`asset_status_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `chk_devices_enabled` CHECK (`enabled` in (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 인스턴스 (현장 1대 = 1행, 얇은 인스턴스층)'
