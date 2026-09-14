CREATE TABLE IF NOT EXISTS `device_image` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `device_id` int(11) NOT NULL,
  `storage_key` varchar(255) NOT NULL,
  `original_name` varchar(255) NOT NULL,
  `content_type` varchar(100) NOT NULL,
  `file_size` bigint(20) NOT NULL,
  `sort_order` int(11) NOT NULL DEFAULT 0,
  `is_primary` tinyint(1) NOT NULL DEFAULT 0,
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_image_storage_key` (`storage_key`),
  KEY `idx_device_image_device_sort` (`device_id`,`is_primary`,`sort_order`),
  CONSTRAINT `fk_device_image_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_device_image_primary` CHECK (`is_primary` in (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 자산 이미지';
