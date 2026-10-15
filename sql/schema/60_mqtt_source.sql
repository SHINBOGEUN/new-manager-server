-- 신규 DB용. 기존 DB는 migration/20261014_unify_mqtt_sources.sql 사용.
CREATE TABLE IF NOT EXISTS `mqtt_source` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'MQTT 수집 소스 ID',
  `name` varchar(100) NOT NULL COMMENT '운영 표시명',
  `source_type` varchar(30) NOT NULL DEFAULT 'CHIRPSTACK_LORA' COMMENT 'CHIRPSTACK_LORA 또는 AGENT',
  `broker_url` varchar(500) NOT NULL COMMENT '브로커 접속 URL',
  `topic` varchar(500) NOT NULL COMMENT 'MQTT 구독 필터',
  `client_id` varchar(255) DEFAULT NULL,
  `credential_key` varchar(100) DEFAULT NULL COMMENT 'Sensor Data 자격증명 키; 비밀번호 미저장',
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `config_version` bigint(20) NOT NULL DEFAULT 1,
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mqtt_source_name` (`name`),
  CONSTRAINT `chk_mqtt_source_type` CHECK (`source_type` IN ('CHIRPSTACK_LORA','AGENT')),
  CONSTRAINT `chk_mqtt_source_enabled` CHECK (`enabled` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공통 MQTT 수집 소스 (LoRa·에이전트)';
