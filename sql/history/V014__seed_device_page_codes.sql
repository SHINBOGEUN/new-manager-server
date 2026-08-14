-- =============================================================================
-- V014: DEVICE_PAGE 공통코드 시드 (장비 노출 페이지)
-- =============================================================================
-- 작성일  : 2026-08-14
-- 대상 DB : MariaDB (dcim_new)
--
-- 적용 방법 (예시):
--   mysql -h HOST -P PORT -u dcim -p dcim_new < sql/history/V014__seed_device_page_codes.sql
--
-- 선행 조건: V002 (code_group), V003 (common_code), V013 (device_page)
--
-- 설계 문서: docs/device/DEVICE_PAGE_API.md
--
-- 역할:
--   - 장비↔페이지 매핑에 쓰는 DEVICE_PAGE 그룹/코드 초기 데이터
--   - 재실행 안전 (이미 있으면 INSERT 생략)
-- =============================================================================

INSERT INTO code_group (group_key, group_name)
SELECT 'DEVICE_PAGE', 'Device Page'
WHERE NOT EXISTS (
    SELECT 1 FROM code_group WHERE group_key = 'DEVICE_PAGE'
);

INSERT INTO common_code (group_id, code, name, sort_order)
SELECT g.id, 'ENVIRONMENT', 'Environment', 1
FROM code_group g
WHERE g.group_key = 'DEVICE_PAGE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.group_id = g.id AND c.code = 'ENVIRONMENT'
  );

INSERT INTO common_code (group_id, code, name, sort_order)
SELECT g.id, 'COOLING', 'Cooling', 2
FROM code_group g
WHERE g.group_key = 'DEVICE_PAGE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.group_id = g.id AND c.code = 'COOLING'
  );

INSERT INTO common_code (group_id, code, name, sort_order)
SELECT g.id, 'ANALYSIS', 'Analysis', 3
FROM code_group g
WHERE g.group_key = 'DEVICE_PAGE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.group_id = g.id AND c.code = 'ANALYSIS'
  );

INSERT INTO common_code (group_id, code, name, sort_order)
SELECT g.id, 'POWER', 'Power', 4
FROM code_group g
WHERE g.group_key = 'DEVICE_PAGE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.group_id = g.id AND c.code = 'POWER'
  );
