-- 48_device_lora_point_override.sql — 장비 단위 LoRa payload 필드 매핑 예외(override)
-- 매핑 우선순위: device_lora_point_override → device_model_lora_point → 없으면 미매핑 오류(lora_ingest_error_log)

CREATE TABLE IF NOT EXISTS `device_lora_point_override` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'LoRa point override ID',
  `device_id` int(11) NOT NULL COMMENT 'devices.id (FK)',
  `payload_field` varchar(255) NOT NULL COMMENT 'payload 내 JSON 경로 (예: object.TempC_SHT)',
  `point_name` varchar(255) NOT NULL COMMENT 'Influx point_name 태그로 쓰일 표준화된 이름',
  `data_point_type_id` int(11) NOT NULL COMMENT 'common_code.id (code_group=DATA_POINT_TYPE)',
  `unit` varchar(50) DEFAULT NULL COMMENT '단위',
  `scale` double DEFAULT NULL COMMENT '값 배율 (NULL=1)',
  `value_map` varchar(1000) DEFAULT NULL COMMENT '상태값→숫자 매핑 JSON (선택)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_lora_point_override_device_field` (`device_id`,`payload_field`),
  UNIQUE KEY `uk_device_lora_point_override_device_name` (`device_id`,`point_name`),
  KEY `idx_device_lora_point_override_data_point_type_id` (`data_point_type_id`),
  CONSTRAINT `fk_device_lora_point_override_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_lora_point_override_data_point_type_id` FOREIGN KEY (`data_point_type_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_device_lora_point_override_enabled` CHECK (`enabled` in (0,1)),
  CONSTRAINT `chk_device_lora_point_override_value_map_json` CHECK (`value_map` is null or json_valid(`value_map`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 단위 LoRa payload 필드 매핑 예외(override)';
