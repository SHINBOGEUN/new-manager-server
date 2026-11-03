ALTER TABLE `device_rack_placement`
  ADD COLUMN IF NOT EXISTS `position_x` double DEFAULT NULL COMMENT '랙 내부 가로 상대 위치 (0~1)',
  ADD COLUMN IF NOT EXISTS `position_y` double DEFAULT NULL COMMENT '랙 내부 높이 상대 위치 (0~1)',
  ADD COLUMN IF NOT EXISTS `position_z` double DEFAULT NULL COMMENT '랙 내부 앞뒤 상대 위치 (0~1)';
