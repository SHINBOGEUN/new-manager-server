-- 08_device_model_modbus_point.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `device_model_modbus_point` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Modbus point ID',
  `model_protocol_id` int(11) NOT NULL COMMENT 'device_model_protocol.id (FK)',
  `name` varchar(255) NOT NULL COMMENT '식별자·표시명 (TOTAL_WT, ONTO-TEMP 등)',
  `register_type` varchar(30) NOT NULL COMMENT '레지스터 종류 (COIL/DISCRETE/HOLDING/INPUT)',
  `data_type` varchar(20) NOT NULL COMMENT '값 해석 타입 (INT16/UINT16/INT32/UINT32/FLOAT32)',
  `byte_order` varchar(10) DEFAULT NULL COMMENT '멀티 레지스터 바이트 순서 (ABCD/CDAB/BADC/DCBA), 단일이면 NULL',
  `address` int(11) DEFAULT NULL COMMENT '레지스터 주소 (0~65535). requires_instance=1이면 NULL',
  `requires_instance` tinyint(1) NOT NULL DEFAULT 0 COMMENT '주소를 인스턴스가 제공하는지 (0=false, 1=true)',
  `scale` double DEFAULT NULL COMMENT '원시값에 곱할 배율 (NULL이면 1)',
  `unit` varchar(50) DEFAULT NULL COMMENT '단위 (W, A, °C, % 등)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_model_modbus_point_protocol_name` (`model_protocol_id`,`name`),
  KEY `idx_device_model_modbus_point_model_protocol_id` (`model_protocol_id`),
  CONSTRAINT `fk_device_model_modbus_point_model_protocol_id` FOREIGN KEY (`model_protocol_id`) REFERENCES `device_model_protocol` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_device_model_modbus_point_requires_instance` CHECK (`requires_instance` in (0,1)),
  CONSTRAINT `chk_device_model_modbus_point_enabled` CHECK (`enabled` in (0,1)),
  CONSTRAINT `chk_device_model_modbus_point_address` CHECK (`requires_instance` = 1 and `address` is null or `requires_instance` = 0 and `address` is not null and `address` between 0 and 65535)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 모델별 Modbus 수집 point (레지스터 카탈로그)'
