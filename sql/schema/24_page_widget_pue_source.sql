CREATE TABLE IF NOT EXISTS `page_widget_pue_source` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `widget_id` int(11) NOT NULL,
  `device_id` int(11) NOT NULL,
  `role` varchar(16) NOT NULL COMMENT 'total | cooler',
  `point_name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_pue_source_widget_device` (`widget_id`,`device_id`),
  KEY `idx_page_widget_pue_source_device` (`device_id`),
  CONSTRAINT `fk_page_widget_pue_source_widget` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_page_widget_pue_source_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`),
  CONSTRAINT `chk_page_widget_pue_source_role` CHECK (`role` in ('total','cooler'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
