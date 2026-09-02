-- 19_page_widget_point.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget_point` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `point_name` varchar(100) NOT NULL COMMENT 'Influx point_name',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_point_widget_name` (`widget_id`,`point_name`),
  KEY `idx_page_widget_point_widget_id` (`widget_id`),
  CONSTRAINT `fk_page_widget_point_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='페이지 위젯 조회 포인트 (1:N)'
