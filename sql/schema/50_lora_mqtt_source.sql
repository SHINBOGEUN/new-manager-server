-- 50_lora_mqtt_source.sql — LoRaWAN/ChirpStack MQTT 수집 소스 설정
-- 비밀번호는 저장하지 않고 credential_key로 Sensor Data 환경변수의 자격증명을 참조한다.

CREATE TABLE IF NOT EXISTS `lora_mqtt_source` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'MQTT 수집 소스 ID',
  `name` varchar(100) NOT NULL COMMENT '운영 화면에 표시할 소스명',
  `source_type` varchar(30) NOT NULL DEFAULT 'CHIRPSTACK_LORA' COMMENT '메시지 형식 분류',
  `broker_url` varchar(500) NOT NULL COMMENT '예: tcp://192.168.0.10:1883',
  `topic` varchar(500) NOT NULL COMMENT '예: application/#',
  `client_id` varchar(255) DEFAULT NULL COMMENT 'MQTT client id (비우면 Sensor Data가 생성)',
  `credential_key` varchar(100) DEFAULT NULL COMMENT 'Sensor Data 환경변수 자격증명 키 (비밀번호 미저장)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
  `config_version` bigint(20) NOT NULL DEFAULT 1 COMMENT '변경 감지용 설정 버전',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lora_mqtt_source_name` (`name`),
  CONSTRAINT `chk_lora_mqtt_source_type` CHECK (`source_type` in ('CHIRPSTACK_LORA')),
  CONSTRAINT `chk_lora_mqtt_source_enabled` CHECK (`enabled` in (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LoRa MQTT 수집 소스 설정';
