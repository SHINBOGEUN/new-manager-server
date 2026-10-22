-- 신규 DB에서 장비별 데이터 감시 모드를 사용할 때 적용
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
