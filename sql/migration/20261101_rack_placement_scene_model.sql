ALTER TABLE `device_rack_placement`
  ADD COLUMN IF NOT EXISTS `scene_model_key` varchar(32) DEFAULT NULL COMMENT '장착 장비의 3D 모델 키';
