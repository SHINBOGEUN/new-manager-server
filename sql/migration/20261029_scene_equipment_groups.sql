ALTER TABLE `device_page_scene_item`
  ADD COLUMN IF NOT EXISTS `group_key` varchar(36) DEFAULT NULL COMMENT '같이 이동하는 설비 묶음 식별자';
