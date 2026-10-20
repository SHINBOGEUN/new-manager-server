-- DEVICE: payload_field는 메시지 루트 기준 경로. LoRa의 object.TempC_SHT 등을 그대로 사용한다.
-- COMPONENT: component_path로 배열을 선택하고 각 항목에서 payload_field와 index/uuid 경로를 읽는다.
-- 예: component_type=GPU, component_path=gpus, payload_field=gpu_temperature,
--     component_index_field=gpu_index, component_uuid_field=gpu_uuid.
-- 실제 GPU 개수·인덱스·UUID별 행을 생성하지 않는다. 페이지별 노출은 기존
-- device_page_model_point_setting(protocol=mqtt)의 point_id로 관리한다.
CREATE TABLE IF NOT EXISTS `device_model_mqtt_point` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'MQTT 모델 포인트 ID',
  `device_model_id` int(11) NOT NULL COMMENT 'device_model.id (FK)',
  `payload_field` varchar(255) NOT NULL COMMENT '루트 또는 구성요소 항목 기준 값의 JSON 경로',
  `point_name` varchar(255) NOT NULL COMMENT 'Influx point_name 태그',
  `data_point_type_id` int(11) NOT NULL COMMENT 'common_code.id (code_group=DATA_POINT_TYPE)',
  `category_code_id` int(11) DEFAULT NULL COMMENT 'common_code.id (code_group=CATEGORY, 측정 항목 분류)',
  `unit_code_id` int(11) DEFAULT NULL COMMENT 'common_code.id (code_group=UNIT)',
  `component_scope` varchar(20) NOT NULL DEFAULT 'DEVICE' COMMENT 'DEVICE 또는 COMPONENT',
  `component_type` varchar(50) DEFAULT NULL COMMENT '구성요소 종류, 예: GPU, CPU',
  `component_path` varchar(255) NOT NULL DEFAULT '' COMMENT '구성요소 배열 경로, DEVICE 범위는 빈 문자열',
  `component_index_field` varchar(255) DEFAULT NULL COMMENT '항목 내 논리 슬롯 식별 경로, 예: gpu_index',
  `component_uuid_field` varchar(255) DEFAULT NULL COMMENT '항목 내 실제 부품 식별 경로, 예: gpu_uuid',
  `scale` double DEFAULT NULL COMMENT '배율 (NULL=1)',
  `value_map` varchar(1000) DEFAULT NULL COMMENT '문자열/상태값 숫자 매핑 JSON',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '모델 포인트 사용 여부',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '포인트 생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '포인트 수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_model_mqtt_point_model_field` (`device_model_id`,`component_path`,`payload_field`),
  UNIQUE KEY `uk_device_model_mqtt_point_model_name` (`device_model_id`,`point_name`),
  KEY `idx_device_model_mqtt_point_data_point_type_id` (`data_point_type_id`),
  KEY `idx_device_model_mqtt_point_category_code_id` (`category_code_id`),
  KEY `idx_device_model_mqtt_point_unit_code_id` (`unit_code_id`),
  CONSTRAINT `fk_device_model_mqtt_point_model_id` FOREIGN KEY (`device_model_id`) REFERENCES `device_model` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_mqtt_point_data_point_type_id` FOREIGN KEY (`data_point_type_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_mqtt_point_category_code_id` FOREIGN KEY (`category_code_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_mqtt_point_unit_code_id` FOREIGN KEY (`unit_code_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_device_model_mqtt_point_enabled` CHECK (`enabled` IN (0,1)),
  CONSTRAINT `chk_device_model_mqtt_point_value_map_json` CHECK (`value_map` IS NULL OR json_valid(`value_map`)),
  CONSTRAINT `chk_device_model_mqtt_point_scope` CHECK (`component_scope` IN ('DEVICE','COMPONENT')),
  CONSTRAINT `chk_device_model_mqtt_point_component` CHECK (
    (`component_scope` = 'DEVICE' AND `component_type` IS NULL AND `component_path` = ''
      AND `component_index_field` IS NULL AND `component_uuid_field` IS NULL)
    OR (`component_scope` = 'COMPONENT' AND `component_type` IS NOT NULL AND CHAR_LENGTH(TRIM(`component_type`)) > 0
      AND CHAR_LENGTH(TRIM(`component_path`)) > 0 AND `component_index_field` IS NOT NULL
      AND CHAR_LENGTH(TRIM(`component_index_field`)) > 0)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='모델별 MQTT 포인트 정의 (LoRa 및 반복 구성요소 지원)';
