-- 13_collection_task_group.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `collection_task_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '그룹 ID',
  `task_id` int(11) NOT NULL COMMENT 'collection_task.id',
  `name` varchar(100) NOT NULL COMMENT '그룹 이름',
  `cron_expression` varchar(100) NOT NULL COMMENT '수집 주기 cron',
  `generated_spec` longtext DEFAULT NULL COMMENT '그룹 수집 JSON spec',
  `collector_job_id` varchar(100) DEFAULT NULL COMMENT 'collector job ID',
  `active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '그룹 활성 여부',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collection_task_group_task_cron` (`task_id`,`cron_expression`),
  UNIQUE KEY `uk_collection_task_group_collector_job_id` (`collector_job_id`),
  KEY `idx_collection_task_group_task_id` (`task_id`),
  CONSTRAINT `fk_collection_task_group_task_id` FOREIGN KEY (`task_id`) REFERENCES `collection_task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_collection_task_group_active` CHECK (`active` in (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Task 안 주기 그룹 (1분/5분 등)'
