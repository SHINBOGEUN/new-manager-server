-- 46_lora_ingest_error_log.sql — LoRa 수집 중 발생한 미등록 식별자·미매핑 필드·변환 실패 이력
-- Sensor Data 서버는 자체 RDB가 없어 Manager API를 통해 이 표에 적재한다.
-- raw_payload는 애플리케이션에서 길이 제한(설정값) 후 저장하며, 보관 기간 정책에 따라 주기적으로 정리한다.

CREATE TABLE IF NOT EXISTS `lora_ingest_error_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '오류 로그 ID',
  `received_at` timestamp(6) NOT NULL COMMENT 'MQTT 메시지 수신 시각',
  `device_id` int(11) DEFAULT NULL COMMENT 'devices.id (FK, 식별자는 매칭됐지만 필드 매핑 실패인 경우)',
  `external_id` varchar(255) DEFAULT NULL COMMENT '수신 payload의 외부 식별자 (추출 실패 시 NULL)',
  `id_type` varchar(20) DEFAULT NULL COMMENT '식별자 종류 (DEV_EUI, DEVICE_NAME)',
  `reason` varchar(500) NOT NULL COMMENT '실패 사유 (UNKNOWN_DEVICE, UNMAPPED_FIELD, CONVERT_FAILED 등)',
  `raw_payload` varchar(4000) DEFAULT NULL COMMENT '원본 payload (설정된 최대 길이로 truncate)',
  `resolved` tinyint(1) NOT NULL DEFAULT 0 COMMENT '운영자 처리 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각 (resolved 처리 등)',
  PRIMARY KEY (`id`),
  KEY `idx_lora_ingest_error_log_resolved_received` (`resolved`,`received_at`),
  KEY `idx_lora_ingest_error_log_external_id` (`external_id`),
  KEY `idx_lora_ingest_error_log_device_id` (`device_id`),
  CONSTRAINT `fk_lora_ingest_error_log_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `chk_lora_ingest_error_log_resolved` CHECK (`resolved` in (0,1)),
  CONSTRAINT `chk_lora_ingest_error_log_id_type` CHECK (`id_type` is null or `id_type` in ('DEV_EUI','DEVICE_NAME'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LoRa 수집 미등록/미매핑/변환실패 오류 이력';
