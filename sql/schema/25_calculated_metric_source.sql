CREATE TABLE IF NOT EXISTS `calculated_metric_source` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '계산 지표 입력 소스 ID',
  `calculated_metric_id` int(11) NOT NULL COMMENT '입력 소스가 속한 계산 지표 ID',
  `device_id` int(11) NOT NULL COMMENT '입력값을 제공하는 장비 ID',
  `alias` varchar(32) NOT NULL COMMENT '계산식에서 참조할 변수명',
  `protocol` varchar(16) NOT NULL COMMENT '원본 측정값 수집 프로토콜',
  `point_name` varchar(100) NOT NULL COMMENT '원본 장비 측정 포인트 이름',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_calculated_metric_source_alias` (`calculated_metric_id`,`alias`),
  KEY `idx_calculated_metric_source_device` (`device_id`),
  CONSTRAINT `fk_calculated_metric_source_metric` FOREIGN KEY (`calculated_metric_id`) REFERENCES `calculated_metric` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_calculated_metric_source_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`),
  CONSTRAINT `chk_calculated_metric_source_protocol` CHECK (`protocol` in ('snmp','modbus'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='계산 지표 alias와 장비 측정 포인트의 연결';
