-- 신규 DB용 최종 스키마. 기존 LoRa DB는 sql/migration/20261014_unify_lora_mqtt.sql을 적용한다.
CREATE TABLE IF NOT EXISTS `device_mqtt_identity` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'MQTT 식별자 매핑 ID',
  `device_id` int(11) NOT NULL COMMENT 'devices.id (FK, Influx device_id)',
  `identity_type` varchar(20) NOT NULL COMMENT 'MAC, DEV_EUI, DEVICE_NAME, UUID, SERIAL',
  `external_id` varchar(255) NOT NULL COMMENT '현재 외부 식별값 원본',
  `normalized_external_id` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '유형별 정규화 식별값, 수신 매칭 기준',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '매칭 사용 여부',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_mqtt_identity_type_normalized` (`identity_type`,`normalized_external_id`),
  UNIQUE KEY `uk_device_mqtt_identity_device_type` (`device_id`,`identity_type`),
  KEY `idx_device_mqtt_identity_device_id` (`device_id`),
  CONSTRAINT `fk_device_mqtt_identity_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_device_mqtt_identity_type` CHECK (`identity_type` IN ('MAC','DEV_EUI','DEVICE_NAME','UUID','SERIAL')),
  CONSTRAINT `chk_device_mqtt_identity_enabled` CHECK (`enabled` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MQTT 외부 식별값과 내부 장비의 현재 연결 (LoRa 포함)';
