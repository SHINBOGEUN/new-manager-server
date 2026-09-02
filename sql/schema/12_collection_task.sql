-- 12_collection_task.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `collection_task` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Task ID',
  `name` varchar(100) NOT NULL COMMENT 'Task 이름',
  `model_id` int(11) NOT NULL COMMENT 'device_model.id',
  `script_type_id` int(11) NOT NULL COMMENT 'common_code.id (PROTOCOL_TYPE)',
  `active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '활성 여부',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collection_task_model_script` (`model_id`,`script_type_id`),
  KEY `idx_collection_task_script_type_id` (`script_type_id`),
  KEY `idx_collection_task_active` (`active`),
  CONSTRAINT `fk_collection_task_model_id` FOREIGN KEY (`model_id`) REFERENCES `device_model` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_collection_task_script_type_id` FOREIGN KEY (`script_type_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `chk_collection_task_active` CHECK (`active` in (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='모델+프로토콜당 수집 Task 1개'
