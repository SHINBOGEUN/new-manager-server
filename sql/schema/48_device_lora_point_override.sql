-- 48_device_lora_point_override.sql — 제거된 장비별 LoRa 매핑 예외 테이블 정리
-- LoRa 수집은 device_model_lora_point의 모델 공통 매핑만 사용한다.
DROP TABLE IF EXISTS `device_lora_point_override`;
