-- 장비별 감시 설정은 수집 방식만 보관하고, 예상 주기는 기존 수집 작업 기준을 사용합니다.
ALTER TABLE `device_monitoring_policy`
  DROP COLUMN `expected_interval_seconds`;
