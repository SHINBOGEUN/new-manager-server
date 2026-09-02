-- =============================================================================
-- V021: aggregate presets (usage / power / pue) + device_role + range_preset
-- =============================================================================
-- 작성일  : 2026-08-31
-- 대상 DB : MariaDB (dcim_new)
--
-- op: delta_sum|weighted_avg|divide → usage|power|pue
-- weight/num/den 컬럼은 유지하되 NULL 로 비움 (미사용)
-- =============================================================================

-- 1) page_widget_aggregate: CHECK 제거 후 op 마이그레이션
ALTER TABLE page_widget_aggregate
    DROP CONSTRAINT chk_page_widget_aggregate_op;

UPDATE page_widget_aggregate
SET op = CASE op
    WHEN 'delta_sum' THEN 'usage'
    WHEN 'divide' THEN 'pue'
    WHEN 'weighted_avg' THEN 'power'
    ELSE op
END
WHERE op IN ('delta_sum', 'divide', 'weighted_avg');

-- 알 수 없는 op 는 usage 로 폴백 (CHECK 통과용)
UPDATE page_widget_aggregate
SET op = 'usage'
WHERE op NOT IN ('usage', 'power', 'pue');

ALTER TABLE page_widget_aggregate
    ADD CONSTRAINT chk_page_widget_aggregate_op CHECK (
        op IN ('usage', 'power', 'pue')
    );

UPDATE page_widget_aggregate
SET weight_point = NULL,
    numerator_point = NULL,
    denominator_point = NULL;

ALTER TABLE page_widget_aggregate
    ADD COLUMN range_preset VARCHAR(16) NULL
        COMMENT 'last_24h | today | yesterday | last_7d | this_month | last_month'
        AFTER denominator_point;

ALTER TABLE page_widget_aggregate
    ADD CONSTRAINT chk_page_widget_aggregate_range_preset CHECK (
        range_preset IS NULL OR range_preset IN (
            'last_24h', 'today', 'yesterday', 'last_7d', 'this_month', 'last_month'
        )
    );

-- usage 기본 기간 today
UPDATE page_widget_aggregate
SET range_preset = 'today'
WHERE op = 'usage' AND range_preset IS NULL;

-- 2) page_widget_device: device_role (NULL = default)
ALTER TABLE page_widget_device
    ADD COLUMN device_role VARCHAR(16) NULL
        COMMENT 'NULL|default | total | it (pue)'
        AFTER device_id;

ALTER TABLE page_widget_device
    ADD CONSTRAINT chk_page_widget_device_role CHECK (
        device_role IS NULL OR device_role IN ('default', 'total', 'it')
    );
