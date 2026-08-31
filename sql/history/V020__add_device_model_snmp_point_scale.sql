-- =============================================================================
-- V020: device_model_snmp_point.scale 추가
-- =============================================================================
-- 작성일  : 2026-08-31
-- 대상 DB : MariaDB (dcim_new)
--
-- 적용 방법 (예시):
--   mysql -h HOST -P PORT -u dcim -p dcim_new < sql/history/V020__add_device_model_snmp_point_scale.sql
--
-- 선행 조건: V006 (device_model_snmp_point)
--
-- 의미: SNMP raw 값에 곱할 배율. NULL이면 collector가 1.0으로 취급.
--       예) raw=5195, scale=0.1, unit=W → MQTT/Influx에는 519.5
-- =============================================================================

ALTER TABLE device_model_snmp_point
    ADD COLUMN scale DOUBLE NULL COMMENT '원시값 배율 (NULL=1)' AFTER unit;
