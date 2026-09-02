-- 18_page_widget_chart.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget_chart` (
  `widget_id` int(11) NOT NULL,
  `chart_scope` varchar(16) NOT NULL,
  `chart_series_mode` varchar(16) NOT NULL,
  `chart_range_preset` varchar(16) NOT NULL,
  `chart_window` varchar(8) NOT NULL,
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`widget_id`),
  CONSTRAINT `fk_pwch_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_pwch_scope` CHECK (`chart_scope` in ('devices','models')),
  CONSTRAINT `chk_pwch_series_mode` CHECK (`chart_series_mode` in ('per_device','sum','by_phase','by_path')),
  CONSTRAINT `chk_pwch_range_preset` CHECK (`chart_range_preset` in ('last_24h','today','yesterday','last_7d','this_month'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
