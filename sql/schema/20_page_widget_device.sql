-- 20_page_widget_device.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget_device` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `device_id` int(11) NOT NULL COMMENT 'devices.id',
  `device_role` varchar(16) DEFAULT NULL COMMENT 'NULL|default',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_device_widget_device` (`widget_id`,`device_id`),
  KEY `idx_page_widget_device_widget_id` (`widget_id`),
  KEY `idx_page_widget_device_device_id` (`device_id`),
  CONSTRAINT `fk_page_widget_device_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_page_widget_device_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_page_widget_device_role` CHECK (`device_role` is null or `device_role` in ('default','total','it'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='페이지 위젯 조회 장비 (1:N)'
