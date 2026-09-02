-- 15_page_widget.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '위젯 ID',
  `page_code_id` int(11) NOT NULL COMMENT 'common_code.id (DEVICE_PAGE)',
  `name` varchar(100) NOT NULL COMMENT '위젯 표시명',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
  `query_kind` varchar(16) NOT NULL COMMENT 'last | aggregate | count',
  `group_by` varchar(16) DEFAULT NULL COMMENT 'device | point | location',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_page_name` (`page_code_id`,`name`),
  KEY `idx_page_widget_page_code_id` (`page_code_id`),
  CONSTRAINT `fk_page_widget_page_code_id` FOREIGN KEY (`page_code_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `chk_page_widget_enabled` CHECK (`enabled` in (0,1)),
  CONSTRAINT `chk_page_widget_group_by` CHECK (`group_by` is null or `group_by` in ('device','point','location')),
  CONSTRAINT `chk_page_widget_query_kind` CHECK (`query_kind` in ('last','aggregate','count','chart'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='페이지 위젯 카드 정의 (DEVICE_PAGE 자식)'
