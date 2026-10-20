-- 기존 threshold JSON 규칙 구조를 정규화 테이블로 전환하는 수동 마이그레이션.
-- 실행 전 백업하고 사전 조회 결과를 확인한다. 이전 임계값/알람 데이터는 *_legacy_json으로 보존한다.
-- 기존 규칙은 새 구조로 자동 변환하지 않으므로 새 UI에서 재등록한다.
-- legacy 테이블명이 이미 있거나 신규 구조가 적용된 DB에는 실행하지 않는다.

SELECT 'threshold_rule' AS table_name, COUNT(*) AS row_count FROM `threshold_rule`
UNION ALL
SELECT 'alarm_event', COUNT(*) FROM `alarm_event`;

RENAME TABLE
  `threshold_rule` TO `threshold_rule_legacy_json`,
  `alarm_event` TO `alarm_event_legacy_json`;

-- RENAME 후 신규 스키마를 순서대로 적용한다:
-- sql/schema/57_threshold_rule.sql
-- sql/schema/58_alarm_event.sql
-- sql/schema/59_threshold_alarm_event.sql
