-- 21_page_widget_model.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget_model` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `model_id` int(11) NOT NULL COMMENT 'device_model.id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_model_widget_model` (`widget_id`,`model_id`),
  KEY `idx_page_widget_model_widget_id` (`widget_id`),
  KEY `idx_page_widget_model_model_id` (`model_id`),
  CONSTRAINT `fk_page_widget_model_model_id` FOREIGN KEY (`model_id`) REFERENCES `device_model` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_page_widget_model_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='chart widget model scope (1:N)'
