-- 22_page_widget_layout.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget_layout` (
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id (PK, 1:1)',
  `grid_x` int(11) NOT NULL COMMENT '그리드 X',
  `grid_y` int(11) NOT NULL COMMENT '그리드 Y',
  `w` int(11) NOT NULL COMMENT '가로 칸 수',
  `h` int(11) NOT NULL COMMENT '세로 칸 수',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`widget_id`),
  CONSTRAINT `fk_page_widget_layout_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_page_widget_layout_grid_x` CHECK (`grid_x` >= 0),
  CONSTRAINT `chk_page_widget_layout_grid_y` CHECK (`grid_y` >= 0),
  CONSTRAINT `chk_page_widget_layout_w` CHECK (`w` >= 1),
  CONSTRAINT `chk_page_widget_layout_h` CHECK (`h` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='페이지 위젯 UI 그리드 배치 2D (1:1)'
