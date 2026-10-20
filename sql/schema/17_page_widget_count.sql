-- 17_page_widget_count.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `page_widget_count` (
  `widget_id` int(11) NOT NULL COMMENT '장비 수 위젯 ID',
  `count_mode` varchar(16) NOT NULL COMMENT '장비 수 집계 방식',
  `count_model_id` int(11) DEFAULT NULL COMMENT '모델별 집계 시 대상 모델 ID',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '설정 생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '설정 수정 시각',
  PRIMARY KEY (`widget_id`),
  KEY `idx_pwc_count_model_id` (`count_model_id`),
  CONSTRAINT `fk_pwc_model_id` FOREIGN KEY (`count_model_id`) REFERENCES `device_model` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_pwc_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_pwc_count_mode` CHECK (`count_mode` in ('total','by_model','model'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 수 위젯 집계 설정'
