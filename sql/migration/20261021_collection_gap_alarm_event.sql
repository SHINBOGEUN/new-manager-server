CREATE TABLE IF NOT EXISTS `collection_gap_alarm_event` (
  `event_id` bigint NOT NULL COMMENT 'alarm_event.id 및 상세 레코드 ID',
  `device_id` int(11) NOT NULL COMMENT '수집 누락이 감지된 장비 ID',
  `protocol` varchar(20) NOT NULL COMMENT '수집 누락을 감시하는 통신 프로토콜 코드',
  PRIMARY KEY (`event_id`),
  KEY `idx_collection_gap_alarm_event_device_protocol` (`device_id`,`protocol`),
  CONSTRAINT `fk_collection_gap_alarm_event_event` FOREIGN KEY (`event_id`) REFERENCES `alarm_event` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_collection_gap_alarm_event_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 데이터 수집 누락 알람 상세';
