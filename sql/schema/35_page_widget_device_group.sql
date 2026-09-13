-- 35_page_widget_device_group.sql — 차트·집계 위젯의 동적 장비 그룹 대상

CREATE TABLE IF NOT EXISTS `page_widget_device_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '위젯 장비 그룹 연결 ID',
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `device_group_id` int(11) NOT NULL COMMENT 'device_group.id',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_device_group` (`widget_id`, `device_group_id`),
  KEY `idx_page_widget_device_group_group` (`device_group_id`),
  CONSTRAINT `fk_page_widget_device_group_widget`
    FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_page_widget_device_group_group`
    FOREIGN KEY (`device_group_id`) REFERENCES `device_group` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='위젯에서 참조하는 장비 그룹';
