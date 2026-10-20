CREATE TABLE IF NOT EXISTS `device_modbus_bit_field` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Modbus 파생 비트 포인트 ID',
    `reading_id` int(11) NOT NULL COMMENT '원본 Modbus reading',
    `point_name` varchar(255) NOT NULL COMMENT '추출 결과 point 이름',
    `bit_offset` int(11) NOT NULL COMMENT 'LSB 기준 시작 비트 (0부터)',
    `bit_width` int(11) NOT NULL COMMENT '추출 비트 수 (1~32)',
    `value_map` text NOT NULL COMMENT '원시 비트값에서 저장값으로 변환하는 JSON 맵',
    `unmapped_value` bigint DEFAULT NULL COMMENT '매핑되지 않은 값의 대체값; NULL이면 원시 추출값 저장',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_modbus_bit_field_reading_name` (`reading_id`, `point_name`),
    CONSTRAINT `fk_modbus_bit_field_reading` FOREIGN KEY (`reading_id`)
        REFERENCES `device_modbus_reading` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_modbus_bit_field_offset` CHECK (`bit_offset` BETWEEN 0 AND 31),
    CONSTRAINT `chk_modbus_bit_field_width` CHECK (`bit_width` BETWEEN 1 AND 32),
    CONSTRAINT `chk_modbus_bit_field_range` CHECK (`bit_offset` + `bit_width` <= 32)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Modbus reading 원본 레지스터의 비트 구간별 파생 point';
