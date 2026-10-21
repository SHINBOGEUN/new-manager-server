-- 기존 단일 임계값/심각도를 주의·위험 기준값으로 분리한다.
-- 실행 전 백업한다. 기존 각 규칙은 현재 심각도에 해당하는 기준값으로 이관한다.

ALTER TABLE `threshold_rule`
  ADD COLUMN `warning_threshold_value` decimal(24,8) NULL COMMENT '주의 경보 기준값 (미사용 시 NULL)' AFTER `operator`,
  ADD COLUMN `critical_threshold_value` decimal(24,8) NULL COMMENT '위험 경보 기준값 (미사용 시 NULL)' AFTER `warning_threshold_value`;

UPDATE `threshold_rule`
SET `warning_threshold_value` = CASE WHEN `severity` = 'WARNING' THEN `threshold_value` ELSE NULL END,
    `critical_threshold_value` = CASE WHEN `severity` = 'CRITICAL' THEN `threshold_value` ELSE NULL END;

ALTER TABLE `threshold_rule`
  DROP CONSTRAINT `chk_threshold_rule_severity`,
  DROP COLUMN `threshold_value`,
  DROP COLUMN `severity`,
  ADD CONSTRAINT `chk_threshold_rule_has_level`
    CHECK (`warning_threshold_value` IS NOT NULL OR `critical_threshold_value` IS NOT NULL),
  ADD CONSTRAINT `chk_threshold_rule_level_order`
    CHECK (`warning_threshold_value` IS NULL OR `critical_threshold_value` IS NULL
      OR (`operator` = 'ABOVE' AND `critical_threshold_value` > `warning_threshold_value`)
      OR (`operator` = 'BELOW' AND `critical_threshold_value` < `warning_threshold_value`)),
  COMMENT = '장비·측정항목별 주의·위험 임계값 규칙';
