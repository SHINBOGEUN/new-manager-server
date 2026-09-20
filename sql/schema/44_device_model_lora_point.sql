-- 44_device_model_lora_point.sql — 모델 단위 LoRa payload 필드 → pointName/타입/단위 매핑 (기본값)
-- payload_field는 JSON 경로 문자열이다. 예: "object.TempC_SHT", "rxInfo[0].rssi"
-- value_map은 문자열/불리언 상태값을 숫자로 바꾸는 JSON 객체다. 예: {"leak":1,"no leak":0}. 없으면 숫자 파싱만 시도한다.

CREATE TABLE IF NOT EXISTS `device_model_lora_point` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'LoRa point 매핑 ID',
  `device_model_id` int(11) NOT NULL COMMENT 'device_model.id (FK)',
  `payload_field` varchar(255) NOT NULL COMMENT 'payload 내 JSON 경로 (예: object.TempC_SHT)',
  `point_name` varchar(255) NOT NULL COMMENT 'Influx point_name 태그로 쓰일 표준화된 이름',
  `data_point_type_id` int(11) NOT NULL COMMENT 'common_code.id (code_group=DATA_POINT_TYPE)',
  `unit` varchar(50) DEFAULT NULL COMMENT '단위 (℃, %RH 등)',
  `scale` double DEFAULT NULL COMMENT '값 배율 (NULL=1)',
  `value_map` varchar(1000) DEFAULT NULL COMMENT '상태값→숫자 매핑 JSON (선택)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_model_lora_point_model_field` (`device_model_id`,`payload_field`),
  UNIQUE KEY `uk_device_model_lora_point_model_name` (`device_model_id`,`point_name`),
  KEY `idx_device_model_lora_point_data_point_type_id` (`data_point_type_id`),
  CONSTRAINT `fk_device_model_lora_point_model_id` FOREIGN KEY (`device_model_id`) REFERENCES `device_model` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_lora_point_data_point_type_id` FOREIGN KEY (`data_point_type_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_device_model_lora_point_enabled` CHECK (`enabled` in (0,1)),
  CONSTRAINT `chk_device_model_lora_point_value_map_json` CHECK (`value_map` is null or json_valid(`value_map`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 모델 단위 LoRa payload 필드 매핑 (기본값)';
