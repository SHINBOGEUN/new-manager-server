-- 14_collection_task_device.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `collection_task_device` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
  `group_id` int(11) NOT NULL COMMENT 'collection_task_group.id',
  `device_id` int(11) NOT NULL COMMENT 'devices.id',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collection_task_device_group_device` (`group_id`,`device_id`),
  KEY `idx_collection_task_device_device_id` (`device_id`),
  CONSTRAINT `fk_collection_task_device_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_collection_task_device_group_id` FOREIGN KEY (`group_id`) REFERENCES `collection_task_group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주기 그룹에 속한 장비. 한 Task 안에서 device는 그룹 1개만'
