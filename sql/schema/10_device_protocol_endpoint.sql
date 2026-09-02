-- 10_device_protocol_endpoint.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `device_protocol_endpoint` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '엔드포인트 ID',
  `device_id` int(11) NOT NULL COMMENT 'devices.id (FK)',
  `protocol_type_id` int(11) NOT NULL COMMENT 'common_code.id (PROTOCOL_TYPE만)',
  `host` varchar(255) NOT NULL COMMENT 'IP 또는 hostname',
  `port` int(11) NOT NULL COMMENT '포트 (1~65535)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_protocol_endpoint_device_protocol` (`device_id`,`protocol_type_id`),
  UNIQUE KEY `uk_device_protocol_endpoint_host_port` (`host`,`port`),
  KEY `idx_device_protocol_endpoint_device_id` (`device_id`),
  KEY `idx_device_protocol_endpoint_protocol_type_id` (`protocol_type_id`),
  CONSTRAINT `fk_device_protocol_endpoint_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_protocol_endpoint_protocol_type_id` FOREIGN KEY (`protocol_type_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `chk_device_protocol_endpoint_enabled` CHECK (`enabled` in (0,1)),
  CONSTRAINT `chk_device_protocol_endpoint_port` CHECK (`port` >= 1 and `port` <= 65535)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 프로토콜 엔드포인트 (host/port 공통 전송층)'
