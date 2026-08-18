-- =============================================================================
-- V015: device_protocol_endpoint (host, port) UK
-- =============================================================================
-- 작성일  : 2026-08-18
-- 대상 DB : MariaDB (dcim_new)
--
-- 적용 방법 (예시):
--   mysql -h HOST -P PORT -u dcim -p dcim_new < sql/history/V015__add_uk_device_protocol_endpoint_host_port.sql
--
-- 선행 조건: V009 (device_protocol_endpoint)
--
-- 설계 문서: docs/device/DEVICE_ENDPOINT_API.md
--
-- 역할:
--   - 수집 목적지(host, port)는 전체 테이블에서 유일
--   - 같은 IP에 SNMP 161 + Modbus 502는 허용 (port가 다름)
--
-- 주의:
--   기존에 (host, port) 중복 행이 있으면 ALTER가 실패합니다.
--   적용 전 중복을 확인하고 한쪽을 수정/삭제하세요.
--
--   SELECT host, port, COUNT(*) AS cnt
--   FROM device_protocol_endpoint
--   GROUP BY host, port
--   HAVING COUNT(*) > 1;
-- =============================================================================

ALTER TABLE device_protocol_endpoint
    ADD UNIQUE KEY uk_device_protocol_endpoint_host_port (host, port);
