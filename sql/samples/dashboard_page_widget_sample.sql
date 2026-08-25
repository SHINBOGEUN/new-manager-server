-- =============================================================================
-- 대시보드 페이지 page_widget 샘플 INSERT (카드 last/aggregate/count)
-- =============================================================================
-- 작성일  : 2026-08-25
-- 대상 DB : MariaDB (dcim_new)
--
-- 적용 방법 (예시):
--   mysql -h HOST -P PORT -u dcim -p dcim_new < sql/samples/dashboard_page_widget_sample.sql
--
-- 선행 조건:
--   - V018 page_widget / page_widget_point
--   - DEVICE_PAGE 코드 `dashboard` (쇼룸 common_code.id = 23)
--
-- 재실행 안전: (page_code_id, name) 있으면 INSERT 생략
-- 차트(24h 전력 등)는 이 파일이 아님. 조회 API.
-- =============================================================================

SET @dashboard_page_id := (
    SELECT cc.id
    FROM common_code cc
    INNER JOIN code_group cg ON cg.id = cc.group_id
    WHERE cg.group_key = 'DEVICE_PAGE'
      AND cc.code = 'dashboard'
    LIMIT 1
);

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, group_by)
SELECT @dashboard_page_id, '전체 전력', 1, 'last', NULL FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = '전체 전력');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, group_by)
SELECT @dashboard_page_id, '전력 구성', 1, 'last', 'location' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = '전력 구성');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind)
SELECT @dashboard_page_id, '칠러', 1, 'last' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = '칠러');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind)
SELECT @dashboard_page_id, '쿨러', 1, 'last' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = '쿨러');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind)
SELECT @dashboard_page_id, 'PDU 실시간', 1, 'last' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'PDU 실시간');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, group_by)
SELECT @dashboard_page_id, '랙 전력 순위', 1, 'last', 'device' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = '랙 전력 순위');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, group_by)
SELECT @dashboard_page_id, '랙 온도 순위', 1, 'last', 'device' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = '랙 온도 순위');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind)
SELECT @dashboard_page_id, 'Dragino', 1, 'last' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'Dragino');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, op)
SELECT @dashboard_page_id, '오늘 kWh', 1, 'aggregate', 'delta_sum' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = '오늘 kWh');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, op)
SELECT @dashboard_page_id, '당월 kWh', 1, 'aggregate', 'delta_sum' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = '당월 kWh');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, op)
SELECT @dashboard_page_id, '전월 kWh', 1, 'aggregate', 'delta_sum' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = '전월 kWh');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, op, weight_point)
SELECT @dashboard_page_id, 'PF', 1, 'aggregate', 'weighted_avg', 'W' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'PF');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, op, numerator_point, denominator_point)
SELECT @dashboard_page_id, 'PUE', 1, 'aggregate', 'divide', 'W', 'IT_POWER' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'PUE');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, op, group_by)
SELECT @dashboard_page_id, '에너지 비율', 1, 'aggregate', 'delta_sum', 'location' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = '에너지 비율');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind)
SELECT @dashboard_page_id, 'PDU 수', 1, 'count' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'PDU 수');

INSERT IGNORE INTO page_widget_point (widget_id, point_name)
SELECT w.id, p.point_name
FROM page_widget w
INNER JOIN (
    SELECT '전체 전력' AS name, 'W' AS point_name
    UNION ALL SELECT '전력 구성', 'W'
    UNION ALL SELECT '칠러', 'status'
    UNION ALL SELECT '칠러', 'W'
    UNION ALL SELECT '칠러', 'in_temp'
    UNION ALL SELECT '칠러', 'out_temp'
    UNION ALL SELECT '쿨러', 'status'
    UNION ALL SELECT '쿨러', 'W'
    UNION ALL SELECT 'PDU 실시간', 'W'
    UNION ALL SELECT 'PDU 실시간', 'PF'
    UNION ALL SELECT 'PDU 실시간', 'AMP'
    UNION ALL SELECT 'PDU 실시간', 'V'
    UNION ALL SELECT 'PDU 실시간', 'L1_WATT'
    UNION ALL SELECT 'PDU 실시간', 'L2_WATT'
    UNION ALL SELECT 'PDU 실시간', 'L3_WATT'
    UNION ALL SELECT '랙 전력 순위', 'W'
    UNION ALL SELECT '랙 온도 순위', 'TEMP1'
    UNION ALL SELECT 'Dragino', 'TempC_SHT'
    UNION ALL SELECT 'Dragino', 'Hum_SHT'
    UNION ALL SELECT '오늘 kWh', 'TOTAL_KWH'
    UNION ALL SELECT '당월 kWh', 'TOTAL_KWH'
    UNION ALL SELECT '전월 kWh', 'TOTAL_KWH'
    UNION ALL SELECT 'PF', 'PF'
    UNION ALL SELECT '에너지 비율', 'TOTAL_KWH'
) p ON p.name = w.name
WHERE w.page_code_id = @dashboard_page_id;

SELECT w.id, w.name, w.query_kind, w.op, w.group_by,
       w.weight_point, w.numerator_point, w.denominator_point,
       p.point_name
FROM page_widget w
LEFT JOIN page_widget_point p ON p.widget_id = w.id
WHERE w.page_code_id = @dashboard_page_id
ORDER BY w.id, p.id;
