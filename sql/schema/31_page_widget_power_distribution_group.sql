CREATE TABLE IF NOT EXISTS `page_widget_power_distribution_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '그룹 ID',
  `widget_id` int(11) NOT NULL COMMENT 'page_widget_power_distribution.widget_id',
  `name` varchar(100) NOT NULL COMMENT '그룹 표시명',
  `color` varchar(16) DEFAULT NULL COMMENT '표시 색상 #RRGGBB',
  `sort_order` int(11) NOT NULL COMMENT '표시 순서',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_power_distribution_group_name` (`widget_id`, `name`),
  KEY `idx_page_widget_power_distribution_group_widget` (`widget_id`),
  CONSTRAINT `fk_page_widget_power_distribution_group_widget`
    FOREIGN KEY (`widget_id`) REFERENCES `page_widget_power_distribution` (`widget_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='전력 분배 위젯 그룹';
