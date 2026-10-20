CREATE TABLE IF NOT EXISTS `alarm_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '알람 이벤트 고유 ID',
  `alarm_type` varchar(40) NOT NULL COMMENT '알람 발생 원인 유형',
  `severity` varchar(20) NOT NULL COMMENT '알람 심각도',
  `visibility` varchar(20) NOT NULL COMMENT '조회 범위 (사용자 공개 또는 관리자 전용)',
  `source_key` varchar(120) NOT NULL COMMENT '같은 알람 원인을 식별하는 키',
  `subject_key` varchar(120) NOT NULL COMMENT '알람 대상 리소스를 식별하는 키',
  `raised_dt` timestamp(6) NOT NULL COMMENT '알람 발생 시각',
  `cleared_dt` timestamp(6) NULL DEFAULT NULL COMMENT '알람 해소 시각 (미해소 시 NULL)',
  PRIMARY KEY (`id`),
  KEY `idx_alarm_event_visibility_raised` (`visibility`,`raised_dt`),
  KEY `idx_alarm_event_source_subject_open` (`source_key`,`subject_key`,`cleared_dt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공통 알람 발생·복구 이력';
