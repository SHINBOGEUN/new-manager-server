-- 49_lora_ingest_error_log.sql — lora_ingest_error_log 제거(기존 운영 DB 정리용)
-- LoRa 영구 오류 이력 기능이 제거되어 이 표는 더 이상 사용하지 않는다.
-- 대체: 장비별 최근 수집 상태는 Sensor Data 메모리(런타임 상태 캐시)로만 보관하고,
-- Manager LoRa 수집 상태 화면은 Sensor Data 내부 API(GET /api/internal/lora/runtime-status)로 조회한다.

DROP TABLE IF EXISTS `lora_ingest_error_log`;
