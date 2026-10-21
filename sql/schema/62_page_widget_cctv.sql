CREATE TABLE IF NOT EXISTS `page_widget_cctv` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'CCTV 위젯 카메라 매핑 ID',
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `camera_id` int(11) NOT NULL COMMENT 'cctv_camera.id',
  `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '위젯 내 카메라 전환 버튼 순서',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_cctv_widget_camera` (`widget_id`,`camera_id`),
  KEY `idx_page_widget_cctv_camera` (`camera_id`),
  CONSTRAINT `fk_page_widget_cctv_widget` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_page_widget_cctv_camera` FOREIGN KEY (`camera_id`) REFERENCES `cctv_camera` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CCTV 위젯과 표시 카메라 연결';
