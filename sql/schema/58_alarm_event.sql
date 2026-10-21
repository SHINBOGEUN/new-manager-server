CREATE TABLE IF NOT EXISTS `alarm_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '알람 이벤트 고유 ID',
  `alarm_type` varchar(40) NOT NULL COMMENT '알람 발생 원인 유형',
  `severity` varchar(20) NOT NULL COMMENT '알람 심각도',
  `visibility` varchar(20) NOT NULL COMMENT '알람 조회 권한 범위 (사용자 공개 또는 관리자 전용)',
  `raised_dt` timestamp(6) NOT NULL COMMENT '알람 발생 시각',
  `cleared_dt` timestamp(6) NULL DEFAULT NULL COMMENT '중복 알람 방지용 내부 해소 시각 (미해소 시 NULL, 화면/API에는 상태로 노출하지 않음)',
  PRIMARY KEY (`id`),
  KEY `idx_alarm_event_visibility_raised` (`visibility`,`raised_dt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='통합 알람 발생 이력';
