-- 43_device_lora_endpoint.sql — LoRa/Dragino 외부 식별자(devEUI 또는 deviceName) ↔ 내부 device 매칭
-- id_type 값은 애플리케이션 상수(DevLoraIdType)와 1:1로 맞춘다. 값이 늘어나면 이 CHECK도 같이 갱신한다.

CREATE TABLE IF NOT EXISTS `device_lora_endpoint` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'LoRa endpoint ID',
  `device_id` int(11) NOT NULL COMMENT 'devices.id (FK)',
  `id_type` varchar(20) NOT NULL COMMENT '외부 식별자 종류 (DEV_EUI, DEVICE_NAME)',
  `external_id` varchar(255) NOT NULL COMMENT '원본 식별자 값 (대소문자·구분자 원본 유지)',
  `normalized_external_id` varchar(255) NOT NULL COMMENT '정규화된 식별자 값 (대문자, 구분자 제거 — 매칭 기준)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_lora_endpoint_type_normalized` (`id_type`,`normalized_external_id`),
  UNIQUE KEY `uk_device_lora_endpoint_device_type` (`device_id`,`id_type`),
  KEY `idx_device_lora_endpoint_device_id` (`device_id`),
  CONSTRAINT `fk_device_lora_endpoint_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_device_lora_endpoint_id_type` CHECK (`id_type` in ('DEV_EUI','DEVICE_NAME')),
  CONSTRAINT `chk_device_lora_endpoint_enabled` CHECK (`enabled` in (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LoRa/Dragino 외부 식별자 ↔ device 매칭';
