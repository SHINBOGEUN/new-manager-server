-- 기존 DB 업그레이드용: 모델 포인트에 분석 차트 분류를 연결한다.
-- category_code_id는 기존 DATA_POINT_TYPE으로 자동 변환하지 않는다.
-- 두 분류의 의미가 다르므로 기존 데이터는 CATEGORY 코드로 별도 지정한다.

ALTER TABLE `device_model_snmp_point`
  ADD COLUMN `category_code_id` int(11) DEFAULT NULL COMMENT 'common_code.id (code_group=CATEGORY, 측정 항목 분류)' AFTER `data_point_type_id`,
  ADD KEY `idx_device_model_snmp_point_category_code_id` (`category_code_id`),
  ADD CONSTRAINT `fk_device_model_snmp_point_category_code_id`
    FOREIGN KEY (`category_code_id`) REFERENCES `common_code` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `device_model_modbus_point`
  ADD COLUMN `category_code_id` int(11) DEFAULT NULL COMMENT 'common_code.id (code_group=CATEGORY, 측정 항목 분류)' AFTER `data_point_type_id`,
  ADD KEY `idx_device_model_modbus_point_category_code_id` (`category_code_id`),
  ADD CONSTRAINT `fk_device_model_modbus_point_category_code_id`
    FOREIGN KEY (`category_code_id`) REFERENCES `common_code` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `device_model_lora_point`
  ADD COLUMN `category_code_id` int(11) DEFAULT NULL COMMENT 'common_code.id (code_group=CATEGORY, 측정 항목 분류)' AFTER `data_point_type_id`,
  ADD KEY `idx_device_model_lora_point_category_code_id` (`category_code_id`),
  ADD CONSTRAINT `fk_device_model_lora_point_category_code_id`
    FOREIGN KEY (`category_code_id`) REFERENCES `common_code` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE;
