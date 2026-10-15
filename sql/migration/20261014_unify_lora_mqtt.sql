-- 기존 DB 전용, 한 번만 실행한다. 신규 DB는 schema/58,59를 사용한다.
-- 사전 조건: 기존 46,47 및 category/unit 컬럼 변경이 적용되어 있어야 한다.
-- Manager를 중지하고 DB 백업 후 실행한다. 대상 공통 테이블이 이미 있으면 실행하지 않는다.
-- MariaDB DDL은 자동 커밋된다. 두 RENAME은 하나의 명령으로 처리하고 이후 ALTER 실패 시
-- 완료된 구문을 확인해 이어서 적용하거나 백업으로 복구한다. 전체 스크립트를 재실행하지 않는다.
-- 행 복사/삭제 없이 이름을 변경하므로 PK, device_id, point_name 및 페이지 설정 point_id가 유지된다.

RENAME TABLE `device_lora_endpoint` TO `device_mqtt_identity`,
             `device_model_lora_point` TO `device_model_mqtt_point`;

ALTER TABLE `device_mqtt_identity`
  DROP CONSTRAINT `chk_device_lora_endpoint_id_type`,
  DROP CONSTRAINT `chk_device_lora_endpoint_enabled`,
  DROP FOREIGN KEY `fk_device_lora_endpoint_device_id`,
  MODIFY COLUMN `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'MQTT 식별자 매핑 ID',
  MODIFY COLUMN `device_id` int(11) NOT NULL COMMENT 'devices.id (FK, Influx device_id)',
  CHANGE COLUMN `id_type` `identity_type` varchar(20) NOT NULL COMMENT 'MAC, DEV_EUI, DEVICE_NAME, UUID, SERIAL',
  MODIFY COLUMN `external_id` varchar(255) NOT NULL COMMENT '현재 외부 식별값 원본',
  MODIFY COLUMN `normalized_external_id` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '유형별 정규화 식별값, 수신 매칭 기준',
  RENAME INDEX `uk_device_lora_endpoint_type_normalized` TO `uk_device_mqtt_identity_type_normalized`,
  RENAME INDEX `uk_device_lora_endpoint_device_type` TO `uk_device_mqtt_identity_device_type`,
  RENAME INDEX `idx_device_lora_endpoint_device_id` TO `idx_device_mqtt_identity_device_id`,
  ADD CONSTRAINT `fk_device_mqtt_identity_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `chk_device_mqtt_identity_type` CHECK (`identity_type` IN ('MAC','DEV_EUI','DEVICE_NAME','UUID','SERIAL')),
  ADD CONSTRAINT `chk_device_mqtt_identity_enabled` CHECK (`enabled` IN (0,1)),
  COMMENT='MQTT 외部 식별값과 내부 장비의 현재 연결 (LoRa 포함)';

ALTER TABLE `device_model_mqtt_point`
  DROP FOREIGN KEY `fk_device_model_lora_point_model_id`,
  DROP FOREIGN KEY `fk_device_model_lora_point_data_point_type_id`,
  DROP FOREIGN KEY `fk_device_model_lora_point_category_code_id`,
  DROP FOREIGN KEY `fk_device_model_lora_point_unit_code_id`,
  DROP CONSTRAINT `chk_device_model_lora_point_enabled`,
  DROP CONSTRAINT `chk_device_model_lora_point_value_map_json`,
  DROP INDEX `uk_device_model_lora_point_model_field`,
  MODIFY COLUMN `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'MQTT 모델 포인트 ID',
  ADD COLUMN `component_scope` varchar(20) NOT NULL DEFAULT 'DEVICE' COMMENT 'DEVICE 또는 COMPONENT' AFTER `unit_code_id`,
  ADD COLUMN `component_type` varchar(50) DEFAULT NULL COMMENT '구성요소 종류, 예: GPU, CPU' AFTER `component_scope`,
  ADD COLUMN `component_path` varchar(255) NOT NULL DEFAULT '' COMMENT '구성요소 배열 경로, DEVICE 범위는 빈 문자열' AFTER `component_type`,
  ADD COLUMN `component_index_field` varchar(255) DEFAULT NULL COMMENT '항목 내 논리 슬롯 식별 경로, 예: gpu_index' AFTER `component_path`,
  ADD COLUMN `component_uuid_field` varchar(255) DEFAULT NULL COMMENT '항목 내 실제 부품 식별 경로, 예: gpu_uuid' AFTER `component_index_field`,
  MODIFY COLUMN `payload_field` varchar(255) NOT NULL COMMENT '루트 또는 구성요소 항목 기준 값의 JSON 경로',
  ADD UNIQUE KEY `uk_device_model_mqtt_point_model_field` (`device_model_id`,`component_path`,`payload_field`),
  RENAME INDEX `uk_device_model_lora_point_model_name` TO `uk_device_model_mqtt_point_model_name`,
  RENAME INDEX `idx_device_model_lora_point_data_point_type_id` TO `idx_device_model_mqtt_point_data_point_type_id`,
  RENAME INDEX `idx_device_model_lora_point_category_code_id` TO `idx_device_model_mqtt_point_category_code_id`,
  RENAME INDEX `idx_device_model_lora_point_unit_code_id` TO `idx_device_model_mqtt_point_unit_code_id`,
  ADD CONSTRAINT `fk_device_model_mqtt_point_model_id` FOREIGN KEY (`device_model_id`) REFERENCES `device_model` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_device_model_mqtt_point_data_point_type_id` FOREIGN KEY (`data_point_type_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_device_model_mqtt_point_category_code_id` FOREIGN KEY (`category_code_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_device_model_mqtt_point_unit_code_id` FOREIGN KEY (`unit_code_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `chk_device_model_mqtt_point_enabled` CHECK (`enabled` IN (0,1)),
  ADD CONSTRAINT `chk_device_model_mqtt_point_value_map_json` CHECK (`value_map` IS NULL OR json_valid(`value_map`)),
  ADD CONSTRAINT `chk_device_model_mqtt_point_scope` CHECK (`component_scope` IN ('DEVICE','COMPONENT')),
  ADD CONSTRAINT `chk_device_model_mqtt_point_component` CHECK (
    (`component_scope` = 'DEVICE' AND `component_type` IS NULL AND `component_path` = ''
      AND `component_index_field` IS NULL AND `component_uuid_field` IS NULL)
    OR (`component_scope` = 'COMPONENT' AND `component_type` IS NOT NULL AND CHAR_LENGTH(TRIM(`component_type`)) > 0
      AND CHAR_LENGTH(TRIM(`component_path`)) > 0 AND `component_index_field` IS NOT NULL
      AND CHAR_LENGTH(TRIM(`component_index_field`)) > 0)
  ),
  COMMENT='모델별 MQTT 포인트 정의 (LoRa 및 반복 구성요소 지원)';

-- 검증: 기존 ID/값/생성 시각이 유지되고 component_scope=DEVICE, component_path=''인지 확인한다.
SELECT `id`, `device_id`, `identity_type`, `external_id`, `normalized_external_id`, `enabled`
FROM `device_mqtt_identity` ORDER BY `id`;
SELECT `id`, `device_model_id`, `point_name`, `payload_field`, `component_scope`, `component_path`
FROM `device_model_mqtt_point` ORDER BY `id`;
