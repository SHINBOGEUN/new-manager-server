CREATE TABLE IF NOT EXISTS `page_widget_last_source` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '최신값 위젯 장비별 측정항목 ID',
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `device_id` int(11) NOT NULL COMMENT 'devices.id',
  `point_name` varchar(100) NOT NULL COMMENT '해당 장비에서 조회할 측정항목명',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_last_source_widget_device_point` (`widget_id`,`device_id`,`point_name`),
  KEY `idx_page_widget_last_source_widget_id` (`widget_id`),
  KEY `idx_page_widget_last_source_device_id` (`device_id`),
  CONSTRAINT `fk_page_widget_last_source_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_page_widget_last_source_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='latest 위젯 장비별 측정항목 매핑';
