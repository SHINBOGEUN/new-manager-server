-- 기존 JSON 알람 보관 테이블과 정규화 alarm_event의 중복 식별 컬럼을 정리한다.
-- 운영 적용 전 DB 백업 및 아래 사전 확인 결과를 검토한다.

-- legacy 테이블 존재 여부 확인 (0이면 이미 없거나 이관되지 않은 상태)
SELECT COUNT(*) AS legacy_table_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'alarm_event_legacy_json';

-- source_key/subject_key는 threshold_alarm_event의 rule/device FK와 중복이다.
ALTER TABLE `alarm_event`
  DROP INDEX `idx_alarm_event_source_subject_open`,
  DROP COLUMN `source_key`,
  DROP COLUMN `subject_key`,
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '알람 이벤트 고유 ID',
  MODIFY COLUMN `alarm_type` varchar(40) NOT NULL COMMENT '알람 발생 원인 유형',
  MODIFY COLUMN `severity` varchar(20) NOT NULL COMMENT '알람 심각도',
  MODIFY COLUMN `visibility` varchar(20) NOT NULL COMMENT '알람 조회 권한 범위 (사용자 공개 또는 관리자 전용)',
  MODIFY COLUMN `raised_dt` timestamp(6) NOT NULL COMMENT '알람 발생 시각',
  MODIFY COLUMN `cleared_dt` timestamp(6) NULL DEFAULT NULL COMMENT '중복 알람 방지용 내부 해소 시각 (미해소 시 NULL, 화면/API에는 상태로 노출하지 않음)',
  COMMENT = '통합 알람 발생 이력';

-- 정규화 이력 구조를 정리한 뒤 과거 JSON 알람 보관 테이블을 삭제한다.
DROP TABLE IF EXISTS `alarm_event_legacy_json`;
