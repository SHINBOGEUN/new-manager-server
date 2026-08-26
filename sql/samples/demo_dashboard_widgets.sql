-- =============================================================================
-- demo_dashboard_widgets: dashboard 페이지 위젯 카드 샘플
-- =============================================================================
-- 작성일  : 2026-08-26
-- 대상 DB : MariaDB (dcim_new)
--
-- 적용 방법:
--   mysql -h HOST -P PORT -u dcim -p dcim_new < sql/samples/demo_dashboard_widgets.sql
--
-- 선행:
--   - seed/REQUIRED_BOOTSTRAP (DEVICE_PAGE.dashboard)
--   - V018 page_widget (+ point / device / layout)
--
-- 이름 규칙: 장비 범위가 보이도록 접두 (PDU / RDC …)
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
SELECT @dashboard_page_id, 'PDU 전체 전력', 1, 'last', NULL FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'PDU 전체 전력');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, group_by)
SELECT @dashboard_page_id, 'PDU 전력 구성', 1, 'last', 'location' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'PDU 전력 구성');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind)
SELECT @dashboard_page_id, '칠러', 1, 'last' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = '칠러');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind)
SELECT @dashboard_page_id, 'RDC 쿨러', 1, 'last' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'RDC 쿨러');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind)
SELECT @dashboard_page_id, 'PDU 실시간', 1, 'last' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'PDU 실시간');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, group_by)
SELECT @dashboard_page_id, 'PDU 랙 전력 순위', 1, 'last', 'device' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'PDU 랙 전력 순위');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, group_by)
SELECT @dashboard_page_id, '랙 온도 순위', 1, 'last', 'device' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = '랙 온도 순위');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind)
SELECT @dashboard_page_id, 'Dragino', 1, 'last' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'Dragino');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, op)
SELECT @dashboard_page_id, 'PDU 오늘 kWh', 1, 'aggregate', 'delta_sum' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'PDU 오늘 kWh');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, op)
SELECT @dashboard_page_id, 'PDU 당월 kWh', 1, 'aggregate', 'delta_sum' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'PDU 당월 kWh');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, op)
SELECT @dashboard_page_id, 'PDU 전월 kWh', 1, 'aggregate', 'delta_sum' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'PDU 전월 kWh');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, op, weight_point)
SELECT @dashboard_page_id, 'PDU PF', 1, 'aggregate', 'weighted_avg', 'W' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'PDU PF');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, op, numerator_point, denominator_point)
SELECT @dashboard_page_id, 'PUE', 1, 'aggregate', 'divide', 'W', 'IT_POWER' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'PUE');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind, op, group_by)
SELECT @dashboard_page_id, 'PDU 에너지 비율', 1, 'aggregate', 'delta_sum', 'location' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'PDU 에너지 비율');

INSERT INTO page_widget (page_code_id, name, enabled, query_kind)
SELECT @dashboard_page_id, 'PDU 수', 1, 'count' FROM DUAL
WHERE @dashboard_page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM page_widget WHERE page_code_id = @dashboard_page_id AND name = 'PDU 수');

INSERT IGNORE INTO page_widget_point (widget_id, point_name)
SELECT w.id, p.point_name
FROM page_widget w
INNER JOIN (
    SELECT 'PDU 전체 전력' AS name, 'W' AS point_name
    UNION ALL SELECT 'PDU 전체 전력', 'TOTAL_WT'
    UNION ALL SELECT 'PDU 전력 구성', 'W'
    UNION ALL SELECT 'PDU 전력 구성', 'TOTAL_WT'
    UNION ALL SELECT '칠러', 'status'
    UNION ALL SELECT '칠러', 'W'
    UNION ALL SELECT '칠러', 'in_temp'
    UNION ALL SELECT '칠러', 'out_temp'
    UNION ALL SELECT 'RDC 쿨러', 'status'
    UNION ALL SELECT 'RDC 쿨러', 'W'
    UNION ALL SELECT 'PDU 실시간', 'W'
    UNION ALL SELECT 'PDU 실시간', 'TOTAL_WT'
    UNION ALL SELECT 'PDU 실시간', 'PF'
    UNION ALL SELECT 'PDU 실시간', 'AMP'
    UNION ALL SELECT 'PDU 실시간', 'V'
    UNION ALL SELECT 'PDU 실시간', 'L1_WATT'
    UNION ALL SELECT 'PDU 실시간', 'L2_WATT'
    UNION ALL SELECT 'PDU 실시간', 'L3_WATT'
    UNION ALL SELECT 'PDU 랙 전력 순위', 'W'
    UNION ALL SELECT 'PDU 랙 전력 순위', 'TOTAL_WT'
    UNION ALL SELECT '랙 온도 순위', 'TEMP1'
    UNION ALL SELECT 'Dragino', 'TempC_SHT'
    UNION ALL SELECT 'Dragino', 'Hum_SHT'
    UNION ALL SELECT 'PDU 오늘 kWh', 'TOTAL_KWH'
    UNION ALL SELECT 'PDU 당월 kWh', 'TOTAL_KWH'
    UNION ALL SELECT 'PDU 전월 kWh', 'TOTAL_KWH'
    UNION ALL SELECT 'PDU PF', 'PF'
    UNION ALL SELECT 'PDU 에너지 비율', 'TOTAL_KWH'
) p ON p.name = w.name
WHERE w.page_code_id = @dashboard_page_id;

-- -----------------------------------------------------------------------------
-- page_widget_device: device_type / 이름 패턴으로 연결 (없으면 skip)
-- -----------------------------------------------------------------------------

INSERT IGNORE INTO page_widget_device (widget_id, device_id)
SELECT w.id, d.id
FROM page_widget w
INNER JOIN devices d ON d.enabled = 1
INNER JOIN device_model dm ON dm.id = d.model_id
INNER JOIN common_code dt ON dt.id = dm.device_type_id
WHERE w.page_code_id = @dashboard_page_id
  AND (
        (w.name IN (
                'PDU 전체 전력', 'PDU 전력 구성', 'PDU 실시간', 'PDU 랙 전력 순위',
                'PDU 오늘 kWh', 'PDU 당월 kWh', 'PDU 전월 kWh', 'PDU PF',
                'PUE', 'PDU 에너지 비율', 'PDU 수'
            )
            AND (LOWER(dt.code) IN ('pdu', 'power') OR d.name LIKE 'PDU%'))
     OR (w.name = '칠러' AND (
            LOWER(dt.code) IN ('chiller', 'cooling')
            OR d.name LIKE '%칠러%' OR d.name LIKE '%Chiller%'))
     OR (w.name = 'RDC 쿨러' AND (
            LOWER(dt.code) IN ('rdc', 'crac', 'cooler', 'cooling')
            OR d.name LIKE '%쿨러%' OR d.name LIKE '%Cooler%' OR d.name LIKE '%RDC%'))
     OR (w.name IN ('랙 온도 순위', 'Dragino') AND (
            LOWER(dt.code) IN ('sensor', 'temp')
            OR d.name LIKE '%SENSOR%' OR d.name LIKE '%Dragino%'))
  );

SELECT w.id, w.name, w.query_kind, w.op, w.group_by,
       pwd.device_id, d.name AS device_name, p.point_name
FROM page_widget w
LEFT JOIN page_widget_device pwd ON pwd.widget_id = w.id
LEFT JOIN devices d ON d.id = pwd.device_id
LEFT JOIN page_widget_point p ON p.widget_id = w.id
WHERE w.page_code_id = @dashboard_page_id
ORDER BY w.id, pwd.device_id, p.id;
