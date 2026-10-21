-- 기존 DB 정리 전용. 신규 설치 스키마가 아니다.
-- device_lora_point_override와 lora_ingest_error_log가 더 이상 사용되지 않는 것을 확인한 뒤
-- 운영 DB 백업 후 한 번만 실행한다. DROP은 되돌릴 수 없으며 MariaDB DDL은 자동 커밋된다.

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('device_lora_point_override', 'lora_ingest_error_log');

DROP TABLE IF EXISTS `device_lora_point_override`;
DROP TABLE IF EXISTS `lora_ingest_error_log`;
