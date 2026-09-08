CREATE TABLE IF NOT EXISTS `page_widget_pue` (
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `range_preset` varchar(32) NOT NULL DEFAULT 'last_24h',
  `freshness_minutes` int(11) NOT NULL DEFAULT 15 COMMENT '마지막 수집값 허용 경과 분',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`widget_id`),
  CONSTRAINT `fk_page_widget_pue_widget` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_page_widget_pue_range` CHECK (`range_preset` in ('last_24h','today','yesterday','last_7d','this_month','last_month')),
  CONSTRAINT `chk_page_widget_pue_freshness` CHECK (`freshness_minutes` between 1 and 1440)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
