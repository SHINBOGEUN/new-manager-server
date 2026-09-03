-- 07_device_model_snmp_point.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `device_model_snmp_point` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'SNMP point ID',
  `model_protocol_id` int(11) NOT NULL COMMENT 'device_model_protocol.id (FK)',
  `name` varchar(255) NOT NULL COMMENT '식별자·표시명 (V, 전압, PRI-FLOW 등)',
  `data_point_type_id` int(11) NOT NULL COMMENT 'common_code.id (code_group=DATA_POINT_TYPE)',
  `oid` varchar(512) NOT NULL COMMENT 'SNMP OID 또는 {instanceId} 템플릿',
  `requires_instance` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'instanceId 치환 필요 여부 (0=false, 1=true)',
  `unit` varchar(50) DEFAULT NULL COMMENT '단위 (V, A, L/min 등)',
  `scale` double DEFAULT NULL COMMENT '(NULL=1)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_model_snmp_point_protocol_name` (`model_protocol_id`,`name`),
  UNIQUE KEY `uk_device_model_snmp_point_protocol_oid` (`model_protocol_id`,`oid`),
  KEY `idx_device_model_snmp_point_model_protocol_id` (`model_protocol_id`),
  KEY `idx_device_model_snmp_point_data_point_type_id` (`data_point_type_id`),
  CONSTRAINT `fk_device_model_snmp_point_data_point_type_id` FOREIGN KEY (`data_point_type_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_snmp_point_model_protocol_id` FOREIGN KEY (`model_protocol_id`) REFERENCES `device_model_protocol` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 모델별 SNMP 수집 point (OID 카탈로그)'
