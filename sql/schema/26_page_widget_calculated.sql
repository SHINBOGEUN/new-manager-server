CREATE TABLE IF NOT EXISTS `page_widget_calculated` (
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `calculated_metric_id` int(11) NOT NULL COMMENT '계산 지표 정의',
  `range_preset` varchar(32) NOT NULL DEFAULT 'last_24h' COMMENT '위젯에 표시할 계산 결과 기간',
  `freshness_minutes` int(11) NOT NULL DEFAULT 15 COMMENT '마지막 계산 결과 허용 경과 분',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`widget_id`),
  KEY `idx_page_widget_calculated_metric` (`calculated_metric_id`),
  CONSTRAINT `fk_page_widget_calculated_widget` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_page_widget_calculated_metric` FOREIGN KEY (`calculated_metric_id`) REFERENCES `calculated_metric` (`id`),
  CONSTRAINT `chk_page_widget_calculated_range` CHECK (`range_preset` in ('last_24h','today','yesterday','last_7d','this_month','last_month')),
  CONSTRAINT `chk_page_widget_calculated_freshness` CHECK (`freshness_minutes` between 1 and 1440)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='계산 지표 위젯의 표시 기간 및 신선도 설정';
