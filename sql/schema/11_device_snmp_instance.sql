-- 11_device_snmp_instance.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `device_snmp_instance` (
  `endpoint_id` int(11) NOT NULL COMMENT 'device_protocol_endpoint.id (PK/FK, SNMP endpoint 1:1)',
  `instance_id` int(11) NOT NULL COMMENT 'SNMP MIB instance index ({instanceId} 치환값)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`endpoint_id`),
  CONSTRAINT `fk_device_snmp_instance_endpoint_id` FOREIGN KEY (`endpoint_id`) REFERENCES `device_protocol_endpoint` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_device_snmp_instance_id` CHECK (`instance_id` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 SNMP instance 인덱스 (OID {instanceId} 치환, 필요한 endpoint만)'
