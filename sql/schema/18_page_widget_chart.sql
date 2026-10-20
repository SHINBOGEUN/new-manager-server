-- 18_page_widget_chart.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget_chart` (
  `widget_id` int(11) NOT NULL COMMENT '차트 위젯 ID',
  `chart_scope` varchar(16) NOT NULL COMMENT '장비 또는 모델 기준 차트 범위',
  `chart_series_mode` varchar(16) NOT NULL COMMENT '시계열 표시·집계 방식',
  `chart_range_preset` varchar(16) NOT NULL COMMENT '차트 기본 조회 기간',
  `chart_window` varchar(8) NOT NULL COMMENT '차트 시간 창 설정',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '설정 생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '설정 수정 시각',
  PRIMARY KEY (`widget_id`),
  CONSTRAINT `fk_pwch_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_pwch_scope` CHECK (`chart_scope` in ('devices','models')),
  CONSTRAINT `chk_pwch_series_mode` CHECK (`chart_series_mode` in ('per_device','sum','by_phase','by_path')),
  CONSTRAINT `chk_pwch_range_preset` CHECK (`chart_range_preset` in ('last_24h','today','yesterday','last_3d','last_7d','this_month','last_month'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='측정 추이 차트 위젯 설정'
