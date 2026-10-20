CREATE TABLE IF NOT EXISTS `threshold_alarm_event` (
  `event_id` bigint NOT NULL COMMENT 'alarm_event.id 및 상세 레코드 ID',
  `threshold_rule_id` bigint NOT NULL COMMENT '알람을 발생시킨 임계값 규칙 ID',
  `device_id` int(11) NOT NULL COMMENT '알람 발생 당시 장비 ID',
  `point_name` varchar(120) NOT NULL COMMENT '알람 발생 당시 측정 포인트',
  `unit` varchar(40) NOT NULL COMMENT '알람 발생 당시 측정 단위',
  `operator` varchar(10) NOT NULL COMMENT '알람 발생 당시 비교 연산자',
  `threshold_value` decimal(24,8) NOT NULL COMMENT '알람 발생 당시 임계값',
  `measured_value` decimal(24,8) NOT NULL COMMENT '알람 발생 당시 측정값',
  PRIMARY KEY (`event_id`),
  KEY `idx_threshold_alarm_event_rule` (`threshold_rule_id`),
  KEY `idx_threshold_alarm_event_device` (`device_id`),
  CONSTRAINT `fk_threshold_alarm_event_event` FOREIGN KEY (`event_id`) REFERENCES `alarm_event` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_threshold_alarm_event_rule` FOREIGN KEY (`threshold_rule_id`) REFERENCES `threshold_rule` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_threshold_alarm_event_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_threshold_alarm_event_operator` CHECK (`operator` IN ('ABOVE','BELOW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='임계값 알람 발생 당시 측정값';
