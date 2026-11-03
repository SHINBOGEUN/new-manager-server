CREATE TABLE IF NOT EXISTS `rack_model_placement` (
  `id` int NOT NULL AUTO_INCREMENT,
  `rack_location_code` char(10) NOT NULL,
  `scene_model_key` varchar(32) NOT NULL,
  `mount_type` varchar(30) NOT NULL,
  `rack_side` varchar(20) DEFAULT NULL,
  `u_position` int DEFAULT NULL,
  `u_height` int DEFAULT NULL,
  `position_x` double DEFAULT NULL,
  `position_y` double DEFAULT NULL,
  `position_z` double DEFAULT NULL,
  `visual_length_ratio` double DEFAULT NULL COMMENT '측면 장착 3D 모델의 랙 높이 대비 길이',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`id`),
  KEY `idx_rack_model_location` (`rack_location_code`),
  CONSTRAINT `fk_rack_model_location` FOREIGN KEY (`rack_location_code`) REFERENCES `location_node` (`code`) ON DELETE CASCADE,
  CONSTRAINT `fk_rack_model_scene` FOREIGN KEY (`scene_model_key`) REFERENCES `scene_model` (`model_key`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='실제 장비 연결 전 랙 모델 배치';
