CREATE TABLE IF NOT EXISTS `page_widget_power_distribution` (
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`widget_id`),
  CONSTRAINT `fk_page_widget_power_distribution_widget`
    FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='전력 분배 위젯 정의';
