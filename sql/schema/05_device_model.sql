-- 05_device_model.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `device_model` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '장비 모델 ID',
  `name` varchar(255) NOT NULL COMMENT '모델/제품명',
  `manufacturer` varchar(255) NOT NULL COMMENT '제조사',
  `device_type_id` int(11) NOT NULL COMMENT 'common_code.id (모델 유형)',
  `description` varchar(1000) DEFAULT NULL COMMENT '설명',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_model_name_manufacturer` (`name`,`manufacturer`),
  KEY `idx_device_model_device_type_id` (`device_type_id`),
  CONSTRAINT `fk_device_model_device_type_id` FOREIGN KEY (`device_type_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 제품 모델 (SKU/제품군)'
