-- 63_device_monitoring_policy.sql — per-device data monitoring mode
CREATE TABLE IF NOT EXISTS `device_monitoring_policy` (
  `device_id` int(11) NOT NULL COMMENT '감시 정책이 적용되는 devices.id',
  `monitoring_mode_code_id` int(11) NOT NULL COMMENT 'DEVICE_MONITORING_MODE 공통코드 ID',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`device_id`),
  KEY `idx_device_monitoring_policy_mode` (`monitoring_mode_code_id`),
  CONSTRAINT `fk_device_monitoring_policy_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_monitoring_policy_mode` FOREIGN KEY (`monitoring_mode_code_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비별 데이터 감시·누락 판정 방식';
