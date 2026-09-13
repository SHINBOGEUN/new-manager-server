-- 34_device_group_device.sql — 장비 그룹과 장비의 다대다 연결

CREATE TABLE IF NOT EXISTS `device_group_device` (
  `device_group_id` int(11) NOT NULL COMMENT 'device_group.id',
  `device_id` int(11) NOT NULL COMMENT 'devices.id',
  PRIMARY KEY (`device_group_id`, `device_id`),
  KEY `idx_device_group_device_device` (`device_id`),
  CONSTRAINT `fk_device_group_device_group`
    FOREIGN KEY (`device_group_id`) REFERENCES `device_group` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_device_group_device_device`
    FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 그룹별 장비 연결';
