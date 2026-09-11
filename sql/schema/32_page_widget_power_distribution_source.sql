CREATE TABLE IF NOT EXISTS `page_widget_power_distribution_source` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '소스 ID',
  `group_id` int(11) NOT NULL COMMENT 'page_widget_power_distribution_group.id',
  `device_id` int(11) NOT NULL COMMENT 'devices.id',
  `point_name` varchar(100) NOT NULL COMMENT 'W 단위 POWER 포인트명',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_power_distribution_source` (`group_id`, `device_id`, `point_name`),
  KEY `idx_page_widget_power_distribution_source_group` (`group_id`),
  KEY `idx_page_widget_power_distribution_source_device` (`device_id`),
  CONSTRAINT `fk_page_widget_power_distribution_source_group`
    FOREIGN KEY (`group_id`) REFERENCES `page_widget_power_distribution_group` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_page_widget_power_distribution_source_device`
    FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='전력 분배 위젯 그룹별 POWER 소스';
