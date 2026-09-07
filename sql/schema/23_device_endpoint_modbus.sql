-- 23_device_endpoint_modbus.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `device_endpoint_modbus` (
  `endpoint_id` int(11) NOT NULL COMMENT 'device_protocol_endpoint.id (PK/FK, Modbus endpoint 1:1)',
  `unit_id` int(11) DEFAULT NULL COMMENT 'Modbus unit/slave ID (0~247). 회선별로 다르면 NULL (매핑 테이블에서 지정)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`endpoint_id`),
  CONSTRAINT `fk_device_endpoint_modbus_endpoint_id` FOREIGN KEY (`endpoint_id`) REFERENCES `device_protocol_endpoint` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_device_endpoint_modbus_unit_id` CHECK (`unit_id` is null or `unit_id` >= 0 and `unit_id` <= 247)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 Modbus 엔드포인트 확장 (unit_id, 필요한 endpoint만)'
