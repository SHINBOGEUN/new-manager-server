-- 장비별 주기 수집 / 상태 변경 수집 정책
-- 기존 장비는 정책 행이 없어도 기존 주기 감시 동작을 유지합니다.
INSERT INTO `code_group` (`group_key`, `group_name`)
VALUES ('DEVICE_MONITORING_MODE', '장비 데이터 감시 방식')
ON DUPLICATE KEY UPDATE `group_name` = VALUES(`group_name`);

INSERT INTO `common_code` (`group_id`, `code`, `name`, `sort_order`, `threshold_supported`)
SELECT g.`id`, 'PERIODIC', '주기 수집', 1, 0
FROM `code_group` g WHERE g.`group_key` = 'DEVICE_MONITORING_MODE'
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `sort_order` = VALUES(`sort_order`);

INSERT INTO `common_code` (`group_id`, `code`, `name`, `sort_order`, `threshold_supported`)
SELECT g.`id`, 'ON_CHANGE', '상태 변경 수집', 2, 0
FROM `code_group` g WHERE g.`group_key` = 'DEVICE_MONITORING_MODE'
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `sort_order` = VALUES(`sort_order`);

CREATE TABLE IF NOT EXISTS `device_monitoring_policy` (
  `device_id` int(11) NOT NULL COMMENT '감시 정책이 적용되는 devices.id',
  `monitoring_mode_code_id` int(11) NOT NULL COMMENT 'DEVICE_MONITORING_MODE 공통코드 ID',
  `expected_interval_seconds` bigint(20) DEFAULT NULL COMMENT '예상 수집 주기(초). PERIODIC에서 지정 시 2배(최소 1시간)가 지나면 누락 알람 판정; NULL이면 기본 기준 사용',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`device_id`),
  KEY `idx_device_monitoring_policy_mode` (`monitoring_mode_code_id`),
  CONSTRAINT `fk_device_monitoring_policy_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_monitoring_policy_mode` FOREIGN KEY (`monitoring_mode_code_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비별 데이터 감시·누락 판정 방식';
