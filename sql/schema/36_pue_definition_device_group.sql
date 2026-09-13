-- 36_pue_definition_device_group.sql — PUE 정의의 역할별 장비 그룹

CREATE TABLE IF NOT EXISTS `pue_definition_device_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'PUE 정의 장비 그룹 연결 ID',
  `pue_definition_id` int(11) NOT NULL COMMENT 'pue_definition.id',
  `device_group_id` int(11) NOT NULL COMMENT 'device_group.id',
  `role` varchar(16) NOT NULL COMMENT 'total 또는 cooler',
  `point_name` varchar(100) NOT NULL COMMENT '그룹 장비가 공통으로 사용하는 POWER 측정항목',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pue_definition_device_group` (`pue_definition_id`, `device_group_id`),
  KEY `idx_pue_definition_device_group_group` (`device_group_id`),
  CONSTRAINT `fk_pue_definition_device_group_definition`
    FOREIGN KEY (`pue_definition_id`) REFERENCES `pue_definition` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pue_definition_device_group_group`
    FOREIGN KEY (`device_group_id`) REFERENCES `device_group` (`id`),
  CONSTRAINT `chk_pue_definition_device_group_role` CHECK (`role` IN ('total', 'cooler'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PUE 정의의 총전력·Cooler 장비 그룹';
