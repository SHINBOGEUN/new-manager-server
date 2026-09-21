-- 51_lora_mqtt_source_status.sql — Sensor Data가 보고하는 MQTT 수집 소스 상태

CREATE TABLE IF NOT EXISTS `lora_mqtt_source_status` (
  `source_id` int(11) NOT NULL COMMENT 'lora_mqtt_source.id (PK/FK)',
  `status` varchar(30) NOT NULL DEFAULT 'NOT_SYNCED' COMMENT 'NOT_SYNCED/CONNECTING/CONNECTED/DISCONNECTED/ERROR/DISABLED',
  `last_connected_at` timestamp(6) NULL DEFAULT NULL,
  `last_message_at` timestamp(6) NULL DEFAULT NULL,
  `last_status_at` timestamp(6) NULL DEFAULT NULL,
  `message_count` bigint(20) NOT NULL DEFAULT 0,
  `error_count` bigint(20) NOT NULL DEFAULT 0,
  `reconnect_count` bigint(20) NOT NULL DEFAULT 0,
  `last_error` varchar(2000) DEFAULT NULL,
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`source_id`),
  CONSTRAINT `fk_lora_mqtt_source_status_source` FOREIGN KEY (`source_id`) REFERENCES `lora_mqtt_source` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_lora_mqtt_source_status` CHECK (`status` in ('NOT_SYNCED','CONNECTING','CONNECTED','DISCONNECTED','ERROR','DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LoRa MQTT 수집 소스 연결/수신 상태';
