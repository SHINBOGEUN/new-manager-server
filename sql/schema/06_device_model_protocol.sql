-- 06_device_model_protocol.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `device_model_protocol` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '모델-프로토콜 연결 ID',
  `model_id` int(11) NOT NULL COMMENT 'device_model.id (FK)',
  `protocol_type_id` int(11) NOT NULL COMMENT 'common_code.id (PROTOCOL_TYPE만)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_model_protocol_model_protocol` (`model_id`,`protocol_type_id`),
  KEY `idx_device_model_protocol_model_id` (`model_id`),
  KEY `idx_device_model_protocol_protocol_type_id` (`protocol_type_id`),
  CONSTRAINT `fk_device_model_protocol_model_id` FOREIGN KEY (`model_id`) REFERENCES `device_model` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_protocol_protocol_type_id` FOREIGN KEY (`protocol_type_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 모델별 지원 프로토콜 (N:M 연결)'
