-- 16_page_widget_aggregate.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget_aggregate` (
  `widget_id` int(11) NOT NULL,
  `op` varchar(16) NOT NULL,
  `weight_point` varchar(100) DEFAULT NULL,
  `numerator_point` varchar(100) DEFAULT NULL,
  `denominator_point` varchar(100) DEFAULT NULL,
  `range_preset` varchar(16) DEFAULT NULL COMMENT 'last_24h | today | yesterday | last_7d | this_month | last_month',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`widget_id`),
  CONSTRAINT `fk_page_widget_aggregate_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_page_widget_aggregate_op` CHECK (`op` in ('usage','power','pue')),
  CONSTRAINT `chk_page_widget_aggregate_range_preset` CHECK (`range_preset` is null or `range_preset` in ('last_24h','today','yesterday','last_7d','this_month','last_month'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
