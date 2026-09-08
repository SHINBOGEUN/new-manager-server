CREATE TABLE IF NOT EXISTS `pue_definition_source` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `pue_definition_id` int(11) NOT NULL,
  `device_id` int(11) NOT NULL,
  `role` varchar(16) NOT NULL,
  `point_name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pue_definition_source_device` (`pue_definition_id`,`device_id`),
  KEY `idx_pue_definition_source_device` (`device_id`),
  CONSTRAINT `fk_pue_definition_source_definition` FOREIGN KEY (`pue_definition_id`) REFERENCES `pue_definition` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pue_definition_source_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`),
  CONSTRAINT `chk_pue_definition_source_role` CHECK (`role` in ('total','cooler'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
