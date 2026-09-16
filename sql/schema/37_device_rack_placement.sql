CREATE TABLE IF NOT EXISTS `device_rack_placement` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `device_id` int(11) NOT NULL,
  `mount_type` varchar(30) NOT NULL DEFAULT 'RACK_U' COMMENT 'RACK_U/RACK_SIDE/RACK_REAR/FLOOR/WALL',
  `rack_location_code` char(10) DEFAULT NULL,
  `rack_side` varchar(20) DEFAULT NULL COMMENT 'LEFT/RIGHT/REAR',
  `u_position` int(11) DEFAULT NULL COMMENT 'RACK_U일 때만 사용',
  `u_height` int(11) DEFAULT NULL COMMENT 'RACK_U일 때만 사용',
  `form_factor` varchar(30) DEFAULT NULL,
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_rack_placement_device` (`device_id`),
  KEY `idx_device_rack_placement_rack_u` (`rack_location_code`,`u_position`),
  CONSTRAINT `fk_device_rack_placement_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_device_rack_placement_location` FOREIGN KEY (`rack_location_code`) REFERENCES `location_node` (`code`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_device_rack_placement_u_position` CHECK (`u_position` IS NULL OR `u_position` >= 1),
  CONSTRAINT `chk_device_rack_placement_u_height` CHECK (`u_height` IS NULL OR `u_height` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 Rack U 배치';
