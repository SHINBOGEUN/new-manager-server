ALTER TABLE `device_page_scene_item`
  ADD COLUMN IF NOT EXISTS `group_name` varchar(80) DEFAULT NULL COMMENT '설비 묶음 이름';
