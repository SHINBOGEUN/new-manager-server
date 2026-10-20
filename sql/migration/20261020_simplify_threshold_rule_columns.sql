-- threshold_rule의 파생 가능한 측정 유형/단위 컬럼을 제거하고 컬럼 설명을 추가한다.
-- 적용 전 DB 백업을 수행한다. 현재 정규화 threshold_rule 구조에만 적용한다.
-- DATA_POINT_TYPE 및 단위는 device measurement source catalog에서 조회한다.

ALTER TABLE `threshold_rule`
  DROP FOREIGN KEY `fk_threshold_rule_data_point_type`,
  DROP INDEX `uk_threshold_rule_device_point`,
  DROP COLUMN `data_point_type_id`,
  DROP COLUMN `unit`,
  ADD UNIQUE KEY `uk_threshold_rule_device_point` (`device_id`, `point_name`),
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '임계값 규칙 고유 ID',
  MODIFY COLUMN `device_id` int(11) NOT NULL COMMENT '임계값을 적용할 장비 ID',
  MODIFY COLUMN `protocol_type_id` int(11) NOT NULL COMMENT '측정값을 수집한 프로토콜 공통코드 ID',
  MODIFY COLUMN `point_name` varchar(120) NOT NULL COMMENT '장비의 측정 포인트 이름',
  MODIFY COLUMN `operator` varchar(10) NOT NULL COMMENT '경보 비교 연산자: ABOVE(초과), BELOW(미만)',
  MODIFY COLUMN `threshold_value` decimal(24,8) NOT NULL COMMENT '경보를 발생시킬 기준값',
  MODIFY COLUMN `severity` varchar(20) NOT NULL COMMENT '경보 심각도: WARNING 또는 CRITICAL',
  MODIFY COLUMN `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '규칙 활성 여부',
  MODIFY COLUMN `created_dt` timestamp(6) NOT NULL DEFAULT current_timestamp(6) COMMENT '규칙 생성 시각',
  MODIFY COLUMN `updated_dt` timestamp(6) NOT NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '규칙 최종 수정 시각',
  COMMENT = '장비·측정항목별 단일 임계값 규칙';
