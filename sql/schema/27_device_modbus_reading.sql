-- 27_device_modbus_reading.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `device_modbus_reading` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
  `endpoint_id` int(11) NOT NULL COMMENT 'device_protocol_endpoint.id — 읽어올 창구 (분전반 등)',
  `point_id` int(11) NOT NULL COMMENT 'device_model_modbus_point.id — 해석 레시피 (requires_instance=1)',
  `unit_id` int(11) NOT NULL COMMENT '이 회선의 Modbus unit/slave ID (0~247)',
  `address` int(11) NOT NULL COMMENT '이 회선의 레지스터 주소 (0~65535)',
  `target_device_id` int(11) NOT NULL COMMENT 'devices.id — 결과를 기록할 장비 (Influx device_id 태그)',
  `point_name` varchar(255) NOT NULL COMMENT '회선별 식별자·표시명 (TOTAL_WT, POWER_A 등)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_modbus_reading_endpoint_unit_address` (`endpoint_id`,`unit_id`,`address`),
  UNIQUE KEY `uk_device_modbus_reading_target_point_name` (`target_device_id`,`point_name`),
  KEY `idx_device_modbus_reading_endpoint_id` (`endpoint_id`),
  KEY `idx_device_modbus_reading_point_id` (`point_id`),i
  KEY `idx_device_modbus_reading_target_device_id` (`target_device_id`),
  CONSTRAINT `fk_device_modbus_reading_endpoint_id` FOREIGN KEY (`endpoint_id`) REFERENCES `device_protocol_endpoint` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_modbus_reading_point_id` FOREIGN KEY (`point_id`) REFERENCES `device_model_modbus_point` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_device_modbus_reading_target_device_id` FOREIGN KEY (`target_device_id`) REFERENCES `devices` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_device_modbus_reading_unit_id` CHECK (`unit_id` between 0 and 247),
  CONSTRAINT `chk_device_modbus_reading_address` CHECK (`address` between 0 and 65535),
  CONSTRAINT `chk_device_modbus_reading_enabled` CHECK (`enabled` in (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Modbus 회선 매핑 (requires_instance=1 point의 회선별 unit/주소 → 대상 장비 point_name)'
